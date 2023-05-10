//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2023.05.08 at 05:12:42 AM PDT 
//


package gov.nih.nci.evs.cdisc.report.xml;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;


/**
 * <p>Java class for ODMcomplexTypeDefinition-GlobalVariables complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ODMcomplexTypeDefinition-GlobalVariables">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element ref="{http://www.cdisc.org/ns/odm/v1.3}StudyName"/>
 *         &lt;element ref="{http://www.cdisc.org/ns/odm/v1.3}StudyDescription"/>
 *         &lt;element ref="{http://www.cdisc.org/ns/odm/v1.3}ProtocolName"/>
 *         &lt;group ref="{http://www.cdisc.org/ns/odm/v1.3}GlobalVariablesElementExtension" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attGroup ref="{http://www.cdisc.org/ns/odm/v1.3}GlobalVariablesAttributeExtension"/>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ODMcomplexTypeDefinition-GlobalVariables", propOrder = {
    "studyName",
    "studyDescription",
    "protocolName"
})
public class ODMcomplexTypeDefinitionGlobalVariables {

    @XmlElement(name = "StudyName", required = true)
    protected ODMcomplexTypeDefinitionStudyName studyName;
    @XmlElement(name = "StudyDescription", required = true)
    protected ODMcomplexTypeDefinitionStudyDescription studyDescription;
    @XmlElement(name = "ProtocolName", required = true)
    protected ODMcomplexTypeDefinitionProtocolName protocolName;

    /**
     * Gets the value of the studyName property.
     * 
     * @return
     *     possible object is
     *     {@link ODMcomplexTypeDefinitionStudyName }
     *     
     */
    public ODMcomplexTypeDefinitionStudyName getStudyName() {
        return studyName;
    }

    /**
     * Sets the value of the studyName property.
     * 
     * @param value
     *     allowed object is
     *     {@link ODMcomplexTypeDefinitionStudyName }
     *     
     */
    public void setStudyName(ODMcomplexTypeDefinitionStudyName value) {
        this.studyName = value;
    }

    /**
     * Gets the value of the studyDescription property.
     * 
     * @return
     *     possible object is
     *     {@link ODMcomplexTypeDefinitionStudyDescription }
     *     
     */
    public ODMcomplexTypeDefinitionStudyDescription getStudyDescription() {
        return studyDescription;
    }

    /**
     * Sets the value of the studyDescription property.
     * 
     * @param value
     *     allowed object is
     *     {@link ODMcomplexTypeDefinitionStudyDescription }
     *     
     */
    public void setStudyDescription(ODMcomplexTypeDefinitionStudyDescription value) {
        this.studyDescription = value;
    }

    /**
     * Gets the value of the protocolName property.
     * 
     * @return
     *     possible object is
     *     {@link ODMcomplexTypeDefinitionProtocolName }
     *     
     */
    public ODMcomplexTypeDefinitionProtocolName getProtocolName() {
        return protocolName;
    }

    /**
     * Sets the value of the protocolName property.
     * 
     * @param value
     *     allowed object is
     *     {@link ODMcomplexTypeDefinitionProtocolName }
     *     
     */
    public void setProtocolName(ODMcomplexTypeDefinitionProtocolName value) {
        this.protocolName = value;
    }

}
