package org.alex859.sax;

import org.alex859.sax.model.Person;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.FileReader;
import java.io.IOException;

public class Main
{
    public static void main(String[] args) throws IOException, SAXException, ParserConfigurationException
    {
        final SAXParser<Person> SAXParser = new SAXParser<>(Person.class, System.out::println);
//        SAXParser.parse(new InputSource(new FileReader("/home/alex859/DevEnv/workspace/sax/src/main/resources/test.xml")));
        SAXParser.parse(new InputSource(new FileReader("/home/alessandro.ciccimarra/DevEnv/saxparser/src/main/resources/test.xml")));
    }
}
