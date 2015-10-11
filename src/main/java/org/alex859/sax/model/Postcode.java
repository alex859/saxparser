package org.alex859.sax.model;

/**
 * @author Alessandro Ciccimarra <alessandro.ciccimarra@gmail.com>
 */
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
