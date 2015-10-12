package org.alex859.sax.handler;

import org.alex859.sax.annotation.XmlTagName;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import java.io.CharArrayWriter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Alessandro Ciccimarra <alessandro.ciccimarra@gmail.com>
 */
class BeanHandler<T> extends DefaultHandler
{
    private final CharArrayWriter contents = new CharArrayWriter();
    private final BeanHandlerFactory beanHandlerFactory;
	private final Class<T> beanType;
	private final Consumer<T> consumer;

	private String beanTypeXmlTagName;
	private Map<String, Field> beanTypeFieldsXmlTagNames;

	private T currentObject;
	private ContentHandler parentHandler;
	private XMLReader xmlReader;

	BeanHandler(final Class<T> beanType, final BeanHandlerFactory beanHandlerFactory, final Consumer<T> consumer)
    {
        this.beanType = beanType;
        this.beanHandlerFactory = beanHandlerFactory;
		this.consumer = consumer;

		initBeanTypeXml();
    }

	private void initBeanTypeXml()
	{
		final XmlTagName beanTypeAnnotation = beanType.getAnnotation(XmlTagName.class);
		this.beanTypeXmlTagName = beanTypeAnnotation != null ? beanTypeAnnotation.value() : beanType.getSimpleName();

		this.beanTypeFieldsXmlTagNames = Arrays.stream(beanType.getDeclaredFields())
				.peek(f -> f.setAccessible(true))
				.collect(Collectors.toMap(f -> {
			final XmlTagName beanTypeFieldAnnotation = f.getAnnotation(XmlTagName.class);
			return beanTypeFieldAnnotation != null ? beanTypeFieldAnnotation.value() : f.getName();
		}, Function.<Field>identity()));
	}

	protected void collect(final T currentObject, final ContentHandler parentHandler)
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

					final Class<?> beanHandlerClass = getBeanHandlerClass(field);

					if (beanHandlerFactory.isClassAllowed(beanHandlerClass))
					{
						final BeanHandler contentHandler = beanHandlerFactory.getRootHandler(beanHandlerClass);
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
			catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException |
					NoSuchFieldException e)
			{
				e.printStackTrace();
			}
		}
		else
		{
			System.out.println("Skipping opening tag: " + qName);
		}

    }

	private boolean isTagKnown(final String qName)
	{
		return !(!qName.equals(beanTypeXmlTagName) && !beanTypeFieldsXmlTagNames.containsKey(qName));
	}

	@Override
    public void endElement(final String uri, final String localName, final String qName) throws SAXException
    {
		if (isTagKnown(qName))
		{
			if (beanType.getSimpleName().equalsIgnoreCase(qName))
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

	@Override
    public void characters(final char[] ch, final int start, final int length) throws SAXException
    {
        contents.write(ch, start, length);
    }

	/**
	 *	Given a field, it returns its type, or, if it is a collection, it returns the type argument
	 */
    protected Class<?> getBeanHandlerClass(final Field field) throws IllegalAccessException, NoSuchMethodException, InvocationTargetException, NoSuchFieldException
    {
		final Class<?> propertyType = field.getType();
        if (Collection.class.isAssignableFrom(propertyType))
        {
			final ParameterizedType listType = (ParameterizedType) field.getGenericType();

			return (Class<?>) listType.getActualTypeArguments()[0];
        }

        return propertyType;
    }

    protected Class<?> getBeanType()
    {
        return beanType;
    }

    public void setXmlReader(final XMLReader xmlReader)
    {
        this.xmlReader = xmlReader;
    }
}
