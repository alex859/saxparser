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

public class SAXParser<ROOT_TYPE>
{
	private final XMLReader xmlReader = XMLReaderFactory.createXMLReader();
	private final Map<Class<?>, BeanHandler<?>> beanHandlersMap;
	private final Consumer<ROOT_TYPE> consumer;
	private final Class<ROOT_TYPE> rootClass;
	private final CharArrayWriter contents = new CharArrayWriter();

	public SAXParser(final Class<ROOT_TYPE> rootClass, final Consumer<ROOT_TYPE> consumer) throws SAXException
	{
		this.consumer = consumer;
		this.rootClass = rootClass;
		this.beanHandlersMap = buildHandlersMap(rootClass);
		this.xmlReader.setContentHandler(getHandler());
	}

	public void parse(final InputSource inputSource) throws IOException, SAXException
	{
		this.xmlReader.parse(inputSource);
	}

	public void setFeature(final String name, final boolean value) throws SAXNotRecognizedException, SAXNotSupportedException
	{
		this.xmlReader.setFeature(name, value);
	}

	private BeanHandler<?> getHandler(final Field field)
    {
		final Class<?> type = getType(field);

		final BeanHandler<?> beanHandler = beanHandlersMap.get(type);
		xmlReader.setContentHandler(beanHandler);

		return beanHandler;
	}

	private BeanHandler<ROOT_TYPE> getHandler()
    {
		return new BeanHandler<>(rootClass, consumer);
    }

	private Map<Class<?>, BeanHandler<?>> buildHandlersMap(final Class<?> rootClass)
	{
		final Map<Class<?>, BeanHandler<?>> map = new HashMap<>();

		return Arrays.stream(rootClass.getDeclaredFields())
				.map(this::getType)
				.filter(isBuiltInType.negate())
				.peek(c -> map.putAll(buildHandlersMap(c)))
				.collect(Collectors.toMap(Function.identity(), c ->
						new BeanHandler<>(c, p -> {})));
	}

	/**
	 * Given a field, it returns its type, or, if it is a collection, it returns the type argument
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

	private boolean isAllowed(final Field field)
	{
		return beanHandlersMap.containsKey(getType(field));
	}

	private static final Set<Class<?>> buildInTypes = Collections.unmodifiableSet(new HashSet<>(Arrays
			.asList(Boolean.class, Byte.class, Character.class, Short.class, Integer.class, Long.class, Double.class,
					Float.class, Void.class, String.class)));

	private Predicate<Class<?>> isBuiltInType =	c -> c.isPrimitive()
			|| buildInTypes.contains(c)
			|| Collection.class.isAssignableFrom(c);

	class BeanHandler<TYPE> extends DefaultHandler
	{
		private final Class<TYPE> beanType;
		private final Consumer<TYPE> consumer;

		private String beanTypeXmlTagName;
		private Map<String, Field> beanTypeFieldsXmlTagNames;

		private TYPE currentObject;
		private BeanHandler<?> parentHandler;

		BeanHandler(final Class<TYPE> beanType, final Consumer<TYPE> consumer)
		{
			this.beanType = beanType;
			this.consumer = consumer;

			initBeanTypeXml();
		}

		private void initBeanTypeXml()
		{
			final XmlType beanTypeAnnotation = beanType.getAnnotation(XmlType.class);
			this.beanTypeXmlTagName = beanTypeAnnotation != null ? beanTypeAnnotation.name() : beanType.getSimpleName();

			this.beanTypeFieldsXmlTagNames = Arrays.stream(beanType.getDeclaredFields())
					.peek(f -> f.setAccessible(true))
					.collect(Collectors.toMap(f -> {
						final XmlElement beanTypeFieldAnnotation = f.getAnnotation(XmlElement.class);
						return beanTypeFieldAnnotation != null ? beanTypeFieldAnnotation.name() : f.getName();
					}, Function.<Field>identity()));
		}

		protected void collect(final TYPE currentObject, final BeanHandler<?> parentHandler)
		{
			this.parentHandler = parentHandler;
			this.currentObject = currentObject;
		}

		@Override
		@SuppressWarnings("unchecked")
		public void startElement(final String uri, final String localName, final String qName, final Attributes attributes) throws SAXException
		{
			contents.reset();

			if (isTagKnown(qName))
			{
				try
				{
					if (beanTypeXmlTagName.equals(qName))
					{
						currentObject = beanType.newInstance();
					}
					else
					{
						final Field field = beanTypeFieldsXmlTagNames.get(qName);
						if (field == null)
						{
							System.out.println("Field not found: " + qName);
							return;
						}

						if (isAllowed(field))
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

							contentHandler.collect(object, this);
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
			if (isTagKnown(qName))
			{
				if (beanTypeXmlTagName.equals(qName))
				{
					if (parentHandler != null)
					{
						xmlReader.setContentHandler(parentHandler);
					}
					consumer.accept(currentObject);
				}
				else
				{
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

		private boolean isTagKnown(final String qName)
		{
			return qName.equals(beanTypeXmlTagName) || beanTypeFieldsXmlTagNames.containsKey(qName);
		}

		@Override
		public void characters(final char[] ch, final int start, final int length) throws SAXException
		{
			contents.write(ch, start, length);
		}

		protected Class<?> getBeanType()
		{
			return beanType;
		}
	}
}