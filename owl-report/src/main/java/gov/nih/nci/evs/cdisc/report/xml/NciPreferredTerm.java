//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2023.05.11 at 05:09:46 AM PDT 
//


package gov.nih.nci.evs.cdisc.report.xml;

import jakarta.xml.bind.annotation.*;

/**
 * <p>Java class for anonymous complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType>
 *   &lt;simpleContent>
 *     &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema>string">
 *       &lt;attribute ref="{http://www.w3.org/1999/02/22-rdf-syntax-ns#}datatype use="required""/>
 *     &lt;/extension>
 *   &lt;/simpleContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "value"
})
@XmlRootElement(name = "nciPreferredTerm")
public class NciPreferredTerm {

    @XmlValue
    protected String value;
    @XmlAttribute(name = "datatype", namespace = "http://www.w3.org/1999/02/22-rdf-syntax-ns#", required = true)
    protected String datatype;

    /**
     * Gets the value of the value property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getValue() {
        return value;
    }

    /**
     * Sets the value of the value property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * Gets the value of the datatype property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDatatype() {
        return datatype;
    }

    /**
     * Sets the value of the datatype property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDatatype(String value) {
        this.datatype = value;
    }

}
