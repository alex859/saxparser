package org.alex859.sax.model;

import javax.xml.bind.annotation.XmlType;

/**
 * @author Alessandro Ciccimarra <alessandro.ciccimarra@gmail.com>
 */
@XmlType(name = "postcode")
public class Postcode
{
    private String first;

    public String getFirst()
    {
        return first;
    }

    public void setFirst(final String first)
    {
        this.first = first;
    }

    @Override
    public String toString()
    {
        return "Postcode{" +
                "first='" + first + '\'' +
                '}';
    }
}
