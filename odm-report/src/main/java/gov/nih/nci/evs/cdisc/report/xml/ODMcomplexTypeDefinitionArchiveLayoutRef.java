//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2023.05.08 at 05:12:42 AM PDT 
//


package gov.nih.nci.evs.cdisc.report.xml;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

/**
 * <p>Java class for ODMcomplexTypeDefinition-ArchiveLayoutRef complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ODMcomplexTypeDefinition-ArchiveLayoutRef">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;group ref="{http://www.cdisc.org/ns/odm/v1.3}ArchiveLayoutRefElementExtension" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attGroup ref="{http://www.cdisc.org/ns/odm/v1.3}ArchiveLayoutRefAttributeExtension"/>
 *       &lt;attGroup ref="{http://www.cdisc.org/ns/odm/v1.3}ArchiveLayoutRefAttributeDefinition"/>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ODMcomplexTypeDefinition-ArchiveLayoutRef")
public class ODMcomplexTypeDefinitionArchiveLayoutRef {

    @XmlAttribute(name = "ArchiveLayoutOID", required = true)
    protected String archiveLayoutOID;

    /**
     * Gets the value of the archiveLayoutOID property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getArchiveLayoutOID() {
        return archiveLayoutOID;
    }

    /**
     * Sets the value of the archiveLayoutOID property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setArchiveLayoutOID(String value) {
        this.archiveLayoutOID = value;
    }

}