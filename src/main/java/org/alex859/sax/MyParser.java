package org.alex859.sax;

import org.alex859.sax.handler.SAXParser;
import org.alex859.sax.model.Person;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

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
		final SAXParser<Person> SAXParser = new SAXParser<>(Person.class, System
            .out::println);

        SAXParser.parse(new InputSource(new FileReader(filename)));
    }


}
