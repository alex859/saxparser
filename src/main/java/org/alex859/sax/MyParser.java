package org.alex859.sax;

import org.alex859.sax.handler.BeanHandlerFactory;
import org.alex859.sax.model.Address;
import org.alex859.sax.model.Person;
import org.alex859.sax.model.Postcode;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import javax.xml.parsers.ParserConfigurationException;
import java.io.FileReader;
import java.io.IOException;

/**
 * @author Alessandro Ciccimarra <alessandro.ciccimarra@gmail.com>
 */
public class MyParser
{
    public static void parse(final String filename) throws ParserConfigurationException, SAXException, IOException
    {
		final XMLReader xmlReader = XMLReaderFactory.createXMLReader();

		final BeanHandlerFactory beanHandlerFactory = new BeanHandlerFactory(xmlReader, Person.class, Address.class, Postcode.class);

        xmlReader.setContentHandler(beanHandlerFactory.getHandler(Person.class, System.out::println));

        xmlReader.parse(new InputSource(new FileReader(filename)));
    }


}
