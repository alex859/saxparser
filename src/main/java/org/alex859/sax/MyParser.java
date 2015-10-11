package org.alex859.sax;

import org.alex859.sax.handler.BeanHandler;
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
import java.util.HashMap;
import java.util.Map;

/**
 * @author Alessandro Ciccimarra <alessandro.ciccimarra@gmail.com>
 */
public class MyParser
{
    private static final Map<Class<?>, BeanHandler> HANDLERS_MAP = new HashMap<>();
    private static final BeanHandler PERSON_HANDLER = new BeanHandler(Person.class, HANDLERS_MAP);
    private static final BeanHandler ADDRESS_HANDLER = new BeanHandler(Address.class, HANDLERS_MAP);
    private static final BeanHandler POSTCODE_HANDLER = new BeanHandler(Postcode.class, HANDLERS_MAP);

    static
    {
        HANDLERS_MAP.put(Person.class, PERSON_HANDLER);
        HANDLERS_MAP.put(Address.class, ADDRESS_HANDLER);
        HANDLERS_MAP.put(Postcode.class, POSTCODE_HANDLER);
    }

    public static void parse(final String filename) throws ParserConfigurationException, SAXException, IOException
    {
        final XMLReader xmlReader = XMLReaderFactory.createXMLReader();
        PERSON_HANDLER.setXmlReader(xmlReader);
        xmlReader.setContentHandler(PERSON_HANDLER);

        xmlReader.parse(new InputSource(new FileReader(filename)));

        System.out.println(PERSON_HANDLER.getCurrentObject());

    }
}
