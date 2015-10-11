package org.alex859.sax.handler;

import org.apache.commons.beanutils.ConstructorUtils;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.logging.log4j.core.config.plugins.util.ResolverUtil;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import java.io.CharArrayWriter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author Alessandro Ciccimarra <alessandro.ciccimarra@gmail.com>
 */
public class BeanHandler extends DefaultHandler
{
    private final CharArrayWriter contents = new CharArrayWriter();
    private Object currentObject;
    private ContentHandler parentHandler;
    private XMLReader xmlReader;
    private final Map<Class<?>, BeanHandler> contentHandlerMap;
    private final Class<?> type;

    public BeanHandler(final Class<?> type, final Map<Class<?>, BeanHandler> contentHandlerMap)
    {
        this.type = type;
        this.contentHandlerMap = contentHandlerMap;
    }

    public void collect(final Object currentObject, final ContentHandler parentHandler, final XMLReader xmlReader)
    {
        this.parentHandler = parentHandler;
        this.xmlReader = xmlReader;
        this.xmlReader.setContentHandler(this);
        this.currentObject = currentObject;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void startElement(final String uri, final String localName, final String qName, final Attributes attributes) throws SAXException
    {
        contents.reset();
        try
        {
            if (type.getSimpleName().equalsIgnoreCase(qName))
            {
                currentObject = ConstructorUtils.invokeConstructor(type, null);
            }
            else
            {
                final BeanHandler contentHandler = contentHandlerMap.get(getBeanHandlerKey(currentObject, qName));
                if (contentHandler != null)
                {
                    final Class<?> type = contentHandler.getType();
                    final Object object = ConstructorUtils.invokeConstructor(type, null);
                    if (Collection.class.isAssignableFrom(PropertyUtils.getPropertyType(currentObject, qName)))
                    {
                        ((Collection) PropertyUtils.getProperty(currentObject, qName)).add(object);
                    }
                    else
                    {
                        PropertyUtils.setProperty(currentObject, qName, object);
                    }

                    contentHandler.collect(object, this, xmlReader);
                }
            }
        }
        catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException |
                NoSuchFieldException e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void endElement(final String uri, final String localName, final String qName) throws SAXException
    {
        if (type.getSimpleName().equalsIgnoreCase(qName))
        {
            xmlReader.setContentHandler(parentHandler);
        }
        else
        {
            try
            {
                PropertyUtils.setProperty(currentObject, qName, contents.toString());
            }
            catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e)
            {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void characters(final char[] ch, final int start, final int length) throws SAXException
    {
        contents.write(ch, start, length);
    }

    protected Class<?> getBeanHandlerKey(final Object currentObject, final String qName) throws IllegalAccessException, NoSuchMethodException, InvocationTargetException, NoSuchFieldException
    {
        final Class<?> propertyType = PropertyUtils.getPropertyType(currentObject, qName);

        if (Collection.class.isAssignableFrom(propertyType))
        {
            final Field listField = currentObject.getClass().getDeclaredField(qName);
            final ParameterizedType listType = (ParameterizedType) listField.getGenericType();
            return (Class<?>) listType.getActualTypeArguments()[0];
        }

        return propertyType;
    }

    public Class<?> getType()
    {
        return type;
    }

    public void setXmlReader(final XMLReader xmlReader)
    {
        this.xmlReader = xmlReader;
    }

    public Object getCurrentObject()
    {
        return currentObject;
    }
}
