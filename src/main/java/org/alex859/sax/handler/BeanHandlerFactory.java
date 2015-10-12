package org.alex859.sax.handler;

import org.xml.sax.XMLReader;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public class BeanHandlerFactory
{
	private final Set<Class<?>> allowedClasses = new HashSet<>();
	private final XMLReader xmlReader;

	public BeanHandlerFactory(final XMLReader xmlReader, final Class<?>... allowedClasses)
	{
		this.setAllowedClasses(allowedClasses);
		this.xmlReader = xmlReader;
	}

	public void setAllowedClasses(final Class<?>... allowedClasses)
	{
		this.allowedClasses.addAll(Arrays.asList(allowedClasses));
	}

	protected <T> BeanHandler<T> getHandler(final Class<T> clazz)
    {
		return getHandler(clazz, o -> {});
    }

	public <T> BeanHandler<T> getHandler(final Class<T> clazz, final Consumer<T> consumer)
    {
		if (isClassAllowed(clazz))
		{
			final BeanHandler<T> beanHandler = new BeanHandler<>(clazz, this, consumer);
			beanHandler.setXmlReader(xmlReader);
			xmlReader.setContentHandler(beanHandler);

			return beanHandler;
		}

		throw new IllegalArgumentException("Class " + clazz.getName() + " is not allowed");
    }

	public boolean isClassAllowed(final Class<?> clazz)
	{
		return allowedClasses.contains(clazz);
	}

}