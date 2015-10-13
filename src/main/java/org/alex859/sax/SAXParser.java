package org.alex859.sax;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Generic class to parse and process an XML document based on {@link XMLReader}.
 * You need to define the topmost XML type you are interested in processing. The desired {@link Consumer} will be used to
 * process the objects as they are parsed from the files.
 * Standard Java {@link XmlType} and {@link XmlElement} can be used to annotate POJOs and define mapping between classes, attributes and XML tags.
 *
 * This class is not thread safe.
 *
 * // TODO Handle errors
 * // TODO Logging
 *
 * @param <TOPMOST_TYPE> The topmost XML type we are interested in
 */
public class SAXParser<TOPMOST_TYPE>
{
	private final XMLReader xmlReader = XMLReaderFactory.createXMLReader();

	/**
	 * Unmodifiable map containing classes and {@link org.alex859.sax.SAXParser.BeanHandler recognised byt the parser}.
	 */
	private final Map<Class<?>, BeanHandler<?>> beanHandlersMap;

	/**
	 * Buffer containing the content of the tags.
	 */
	private final CharArrayWriter contents = new CharArrayWriter();

	/**
	 * A SAXParser is defined by the topmost class we are interested in and the consumer logic that will be applied to objects created during parsing.
	 * Both parameters and this class itself are parametrized to be sure that types are coherent.
	 *
	 * @param topMostClass The class type we want to process.
	 * @param consumer The consumer logic to process the POJOs we parse.
	 * @throws SAXException
	 */
	public SAXParser(final Class<TOPMOST_TYPE> topMostClass, final Consumer<TOPMOST_TYPE> consumer) throws SAXException
	{
		this.beanHandlersMap = Collections.unmodifiableMap(buildHandlersMap(topMostClass));
		this.xmlReader.setContentHandler(new BeanHandler<>(topMostClass, consumer));
	}

	/**
	 * @see {@link XMLReader#parse(InputSource)}.
	 */
	public void parse(final InputSource inputSource) throws IOException, SAXException
	{
		this.xmlReader.parse(inputSource);
	}

	/**
	 * @see {@link XMLReader#setFeature(String, boolean)}.
	 */
	public void setFeature(final String name, final boolean value) throws SAXNotRecognizedException, SAXNotSupportedException
	{
		this.xmlReader.setFeature(name, value);
	}

	/**
	 * Looks for the correct handler to use.
	 *
	 * @param field The field we want to find the handler for.
	 * @return The handler for the field's type.
	 */
	private BeanHandler<?> getHandler(final Field field)
    {
		final Class<?> type = getType(field);
		return beanHandlersMap.get(type);
	}

	/**
	 * Recursively scans the root class creating {@link org.alex859.sax.SAXParser.BeanHandler}s for all non built in types.
	 * @see {@link SAXParser#isBuiltInType}.
	 *
	 * @param rootClass The class we start the exploration from.
	 * @return A {@link Class} -> {@link org.alex859.sax.SAXParser.BeanHandler} mapping for the parser.
	 */
	private Map<Class<?>, BeanHandler<?>> buildHandlersMap(final Class<?> rootClass)
	{
		final Map<Class<?>, BeanHandler<?>> map = new HashMap<>();

		return Arrays.stream(rootClass.getDeclaredFields())
				.map(this::getType)
				.filter(isBuiltInType.negate())
				.peek(c -> map.putAll(buildHandlersMap(c)))
				.collect(Collectors.toMap(Function.identity(), c ->	new BeanHandler<>(c)));
	}

	/**
	 * @param field The field we are interested in.
	 * @return The field's type, or, if it is a collection, its type argument.
	 */
	private Class<?> getType(final Field field)
	{
		final Class<?> propertyType = field.getType();
		if (Collection.class.isAssignableFrom(propertyType))
		{
			final ParameterizedType listType = (ParameterizedType) field.getGenericType();

			return (Class<?>) listType.getActualTypeArguments()[0];
		}

		return propertyType;
	}

	private boolean isFieldTypeAllowed(final Field field)
	{
		// should always return true since we control the map's creation
		return beanHandlersMap.containsKey(getType(field));
	}

	private static final Set<Class<?>> buildInTypes = Collections.unmodifiableSet(new HashSet<>(Arrays
			.asList(Boolean.class, Byte.class, Character.class, Short.class, Integer.class, Long.class, Double.class,
					Float.class, Void.class, String.class)));

	private Predicate<Class<?>> isBuiltInType =	c -> c.isPrimitive()
			|| buildInTypes.contains(c)
			|| Collection.class.isAssignableFrom(c);


	/**
	 * Receives SAX events as it reaches start/end/content of a tag.
	 * It keeps reference to a hierarchy of handler to handle the different types needed by the TOPMOST_TYPE.
	 *
	 * @param <TYPE> The type that can be handled.
	 *
	 * @see {@link DefaultHandler}.
	 */
	class BeanHandler<TYPE> extends DefaultHandler
	{
		private final Class<TYPE> beanType;
		private final Consumer<TYPE> consumer;

		/**
		 * @see {@link BeanHandler#initBeanTypeXml()}.
		 */
		private String beanTypeXmlTagName;
		/**
		 * @see {@link BeanHandler#initBeanTypeXml()}.
		 */
		private Map<String, Field> beanTypeFieldsXmlTagNames;

		/**
		 * Reference to the object currently being created.
		 */
		private TYPE currentObject;

		/**
		 * Reference to the parent handler. When we finish processing this handler's root tag, control will be passed to this parent handler.
		 */
		private BeanHandler<?> parentHandler;

		/**
		 * Creates a {@link org.alex859.sax.SAXParser.BeanHandler} with the specified {@link Class} and {@link Consumer}.
		 *
		 * @param beanType The type associated to this handler.
		 * @param consumer The logic to process created POJOs with.
		 */
		BeanHandler(final Class<TYPE> beanType, final Consumer<TYPE> consumer)
		{
			this.beanType = beanType;
			this.consumer = consumer;

			initBeanTypeXml();
		}

		/**
		 * Creates a {@link org.alex859.sax.SAXParser.BeanHandler} with a default no-op {@link Consumer}.
		 *
		 * @param beanType The type associated to this handler.
		 */
		BeanHandler(final Class<TYPE> beanType)
		{
			this(beanType, b -> {});
		}

		/**
		 * Scans POJOs class annotations to create a mapping XML tags -> Field
		 */
		private void initBeanTypeXml()
		{
			final XmlType beanTypeAnnotation = beanType.getAnnotation(XmlType.class);
			this.beanTypeXmlTagName = beanTypeAnnotation != null ? beanTypeAnnotation.name() : beanType.getSimpleName();

			this.beanTypeFieldsXmlTagNames = Arrays.stream(beanType.getDeclaredFields())
					.peek(f -> f.setAccessible(true))
					.collect(Collectors.toMap(f -> {
						final XmlElement beanTypeFieldAnnotation = f.getAnnotation(XmlElement.class);
						return beanTypeFieldAnnotation != null ? beanTypeFieldAnnotation.name() : f.getName();
					}, Function.identity()));
		}

		@Override
		@SuppressWarnings("unchecked")
		public void startElement(final String uri, final String localName, final String qName, final Attributes attributes) throws SAXException
		{
			contents.reset();

			if (isTagAllowed(qName))
			{
				try
				{
					if (beanTypeXmlTagName.equals(qName))
					{
						// I am responsible for this tag!
						currentObject = beanType.newInstance();
					}
					else
					{
						// Delegate the responsibility to an inner handler..depending on the current field
						final Field field = beanTypeFieldsXmlTagNames.get(qName);
						if (field == null)
						{
							// Mapping configuration error
							System.out.println("Field not found: " + qName);
							return;
						}

						if (isFieldTypeAllowed(field)) // TODO just to make sure that the contentHandler is not null..should always be true
						{
							final BeanHandler contentHandler = getHandler(field);
							final Class<?> type = contentHandler.getBeanType();
							final Object object = type.newInstance();
							if (Collection.class.isAssignableFrom(field.getType()))
							{
								((Collection) field.get(currentObject)).add(object);
							}
							else
							{
								field.set(currentObject, object);
							}

							// Control is passed to the inner handler
							contentHandler.parentHandler = this;
							contentHandler.currentObject = object;
							xmlReader.setContentHandler(contentHandler);
						}

					}
				}
				catch (IllegalAccessException | InstantiationException e)
				{
					e.printStackTrace();
				}
			}
			else
			{
				System.out.println("Skipping opening tag: " + qName);
			}

		}

		@Override
		public void endElement(final String uri, final String localName, final String qName) throws SAXException
		{
			if (isTagAllowed(qName))
			{
				if (beanTypeXmlTagName.equals(qName))
				{
					if (parentHandler != null)
					{
						// control goes back to the parent handler
						xmlReader.setContentHandler(parentHandler);
					}

					// we finished parsing the tag...let's call the processing logic
					consumer.accept(currentObject);
				}
				else
				{
					// populate builtin types fields
					try
					{
						final Field field = beanTypeFieldsXmlTagNames.get(qName);
						field.set(currentObject, contents.toString());
					}
					catch (IllegalAccessException e)
					{
						e.printStackTrace();
					}
				}
			}
			else
			{
				System.out.println("Skipping closing tag: " + qName);
			}
		}

		@Override
		public void characters(final char[] ch, final int start, final int length) throws SAXException
		{
			contents.write(ch, start, length);
		}

		private boolean isTagAllowed(final String qName)
		{
			return qName.equals(beanTypeXmlTagName) || beanTypeFieldsXmlTagNames.containsKey(qName);
		}

		private Class<?> getBeanType()
		{
			return beanType;
		}
	}
}