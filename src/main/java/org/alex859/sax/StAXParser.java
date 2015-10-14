package org.alex859.sax;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.CharArrayWriter;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static javax.xml.stream.XMLStreamConstants.CHARACTERS;
import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
import static javax.xml.stream.XMLStreamConstants.START_ELEMENT;

/**
 * Generic class to parse and process an XML document based on Cursor StAX API {@link XMLStreamReader}.
 * You need to define the topmost XML type you are interested in processing. The desired {@link Consumer} will be used to
 * process the objects as they are parsed from the files.
 * Standard Java {@link XmlType} and {@link XmlElement} can be used to annotate POJOs and define mapping between classes, attributes and XML tags.
 * <p>
 * This class is not thread safe.
 * <p>
 * // TODO Handle errors
 * // TODO Logging
 *
 * @param <TYPE> The topmost XML type we are interested in
 */
public class StAXParser<TYPE> implements Iterator<TYPE>
{
	private final XMLStreamReader reader;
	private final FileInputStream fileInputStream;

	private EventHandler<TYPE> eventHandler;

	/**
	 * The current bean handler changes as we find tags that are mapped to custom types.
	 */
	private EventHandler<?> currentEventHandler;

	/**
	 * Buffer containing the content of the tags.
	 */
	private final CharArrayWriter contents = new CharArrayWriter();

	/**
	 * Unmodifiable map containing classes and {@link org.alex859.sax.SAXParser.BeanHandler recognised byt the parser}.
	 */
	private final Map<Class<?>, EventHandler<?>> beanHandlersMap;

	/**
	 * A StAXParser is defined by the topmost class we are interested in and the file to process.
	 *
	 * @param topMostClass    The class type we want to process.
	 * @param fileInputStream File to process;
	 */
	public StAXParser(final Class<TYPE> topMostClass, final FileInputStream fileInputStream) throws XMLStreamException
	{
		final XMLInputFactory factory = XMLInputFactory.newFactory();
		this.fileInputStream = fileInputStream;
		reader = factory.createXMLStreamReader(fileInputStream);
		this.beanHandlersMap = Collections.unmodifiableMap(buildHandlersMap(topMostClass));
		this.eventHandler = new EventHandler<>(topMostClass);
		this.currentEventHandler = this.eventHandler;
	}

	/**
	 * Looks for the correct handler to use.
	 *
	 * @param field The field we want to find the handler for.
	 * @return The handler for the field's type.
	 */
	private EventHandler<?> getHandler(final Field field)
	{
		final Class<?> type = getType(field);
		return beanHandlersMap.get(type);
	}

	/**
	 * Recursively scans the root class creating {@link org.alex859.sax.SAXParser.BeanHandler}s for all non built in types.
	 *
	 * @param rootClass The class we start the exploration from.
	 * @return A {@link Class} -> {@link org.alex859.sax.SAXParser.BeanHandler} mapping for the parser.
	 * @see {@link SAXParser#isBuiltInType}.
	 */
	private Map<Class<?>, EventHandler<?>> buildHandlersMap(final Class<?> rootClass)
	{
		final Map<Class<?>, EventHandler<?>> map = new HashMap<>();

		return Arrays.stream(rootClass.getDeclaredFields())
				.map(this::getType)
				.filter(isBuiltInType.negate())
				.peek(c -> map.putAll(buildHandlersMap(c)))
				.collect(Collectors.toMap(Function.identity(), c -> new EventHandler<>(c)));
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

	private Predicate<Class<?>> isBuiltInType = c -> c.isPrimitive()
			|| buildInTypes.contains(c)
			|| Collection.class.isAssignableFrom(c);


	/**
	 * @return The next parsed object
	 */
	@Override
	public TYPE next()
	{
		try
		{
			if (reader.hasNext())
			{
				while (reader.hasNext() && !isEndTag())
				{
					currentEventHandler.processCurrentEvent();
					reader.next();
				}
				return eventHandler.getCurrentObject();
			}
		}
		catch (final XMLStreamException e)
		{
			//	TODO
			return null;
		}

		return null;
	}

	/**
	 * @return {@code true} if the stream contains another object, {@code false} otherwise
	 */
	@Override
	public boolean hasNext()
	{
		try
		{
			goToNextStartTagOrToEnd();
		}
		catch (XMLStreamException e)
		{
			return false;
		}

		return isStartTag();
	}

	private void goToNextStartTagOrToEnd() throws XMLStreamException
	{
		while (reader.hasNext() && !isStartTag())
		{
			reader.next();
		}

		if (!reader.hasNext())
		{
			close();
		}
	}

	private boolean isStartTag()
	{
		return reader.getEventType() == START_ELEMENT && eventHandler.getBeanTypeXmlTagName().equals(reader.getLocalName());
	}

	private boolean isEndTag()
	{
		return reader.getEventType() == END_ELEMENT && eventHandler.getBeanTypeXmlTagName().equals(reader.getLocalName());
	}

	private void close()
	{
		try
		{
			reader.close();
			fileInputStream.close();

		}
		catch (XMLStreamException  | IOException e)
		{
			// TODO
		}
	}


	/**
	 * Receives StAX events as it reaches start/end/content of a tag.
	 * It keeps reference to a hierarchy of handler to handle the different types needed by the topmost TYPE.
	 *
	 * @param <T> The type that can be handled.
	 */
	class EventHandler<T>
	{
		private final Class<T> beanType;

		/**
		 * @see {@link EventHandler#initBeanTypeXml()}.
		 */
		private String beanTypeXmlTagName;
		/**
		 * @see {@link EventHandler#initBeanTypeXml()}.
		 */
		private Map<String, Field> beanTypeFieldsXmlTagNames;

		/**
		 * Reference to the object currently being created.
		 */
		private T currentObject;

		/**
		 * Reference to the parent handler. When we finish processing this handler's root tag, control will be passed to this parent handler.
		 */
		private EventHandler<?> parentHandler;

		/**
		 * Creates a {@link org.alex859.sax.SAXParser.BeanHandler} with the specified {@link Class} and {@link Consumer}.
		 *
		 * @param beanType The type associated to this handler.
		 */
		EventHandler(final Class<T> beanType)
		{
			this.beanType = beanType;

			initBeanTypeXml();
		}

		public void processCurrentEvent()
		{
			switch (reader.getEventType())
			{
				case START_ELEMENT:
					startElement(reader.getLocalName());
					break;
				case CHARACTERS:
					characters(reader.getText());
					break;
				case END_ELEMENT:
					endElement(reader.getLocalName());
					break;
			}
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

		@SuppressWarnings("unchecked")
		private void startElement(final String localName)
		{
			contents.reset();

			if (isTagAllowed(localName))
			{
				try
				{
					if (beanTypeXmlTagName.equals(localName))
					{
						// I am responsible for this tag!
						currentObject = beanType.newInstance();
					}
					else
					{
						// Delegate the responsibility to an inner handler..depending on the current field
						final Field field = beanTypeFieldsXmlTagNames.get(localName);
						if (field == null)
						{
							// Mapping configuration error
							System.out.println("Field not found: " + localName);
							return;
						}

						if (isFieldTypeAllowed(field)) // TODO just to make sure that the contentHandler is not null..should always be true
						{
							final EventHandler contentHandler = getHandler(field);
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
							currentEventHandler = contentHandler;
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
				System.out.println("Skipping opening tag: " + localName);
			}

		}

		private void endElement(final String localName)
		{
			if (isTagAllowed(localName))
			{
				if (beanTypeXmlTagName.equals(localName))
				{
					if (parentHandler != null)
					{
						// control goes back to the parent handler
						currentEventHandler = parentHandler;
					}
				}
				else
				{
					// populate builtin types fields
					try
					{
						final Field field = beanTypeFieldsXmlTagNames.get(localName);
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
				System.out.println("Skipping closing tag: " + localName);
			}
		}

		private void characters(final String content)
		{
			try
			{
				contents.write(content);
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}

		public String getBeanTypeXmlTagName()
		{
			return beanTypeXmlTagName;
		}

		private T getCurrentObject()
		{
			return currentObject;
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
