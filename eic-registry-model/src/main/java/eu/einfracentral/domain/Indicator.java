package eu.einfracentral.domain;

import io.swagger.annotations.ApiModelProperty;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.Arrays;
import java.util.List;

@XmlType
@XmlRootElement(namespace = "http://einfracentral.eu")
public class Indicator implements Identifiable {
    @XmlElement(required = true)
    @ApiModelProperty(position = 1, example = "Indicator's ID")
    private String id;
    @XmlElement(required = true)
    @ApiModelProperty(position = 2, example = "Indicator's short description")
    private String description;
    @XmlElementWrapper(name = "dimensions")
    @XmlElement(name = "dimension")
    @ApiModelProperty(position = 3, example = "['TIME', 'LOCATION'] (at least one)", required = true)
    private List<DimensionType> dimensions;
    @XmlElement(name = "unit")
    @ApiModelProperty(position = 4, example = "'PCT', 'NUM' or 'BOOL'", required = true)
    private UnitType unit;

    public Indicator() {

    }

    public Indicator(Indicator indicator) {
        this.id = indicator.getId();
        this.description = indicator.getDescription();
        this.unit = indicator.getUnit();
        this.dimensions = indicator.getDimensions();
    }


    public enum UnitType {
        PCT("percentage"),
        NUM("numeric"),
        BOOL("boolean");

        private final String unitType;

        UnitType(final String unitType) {
            this.unitType = unitType;
        }

        public String getKey() {
            return unitType;
        }

        /**
         * @return the Enum representation for the given string.
         * @throws IllegalArgumentException if unknown string.
         */
        public static UnitType fromString(String s) throws IllegalArgumentException {
            return Arrays.stream(UnitType.values())
                    .filter(v -> v.unitType.equals(s))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("unknown value: " + s));
        }

    }

    public enum DimensionType {
        TIME("time"),
        LOCATION("location");

        private final String dimensionType;

        DimensionType(final String dimensionType) {
            this.dimensionType = dimensionType;
        }

        public String getKey() {
            return dimensionType;
        }

        /**
         * @return the Enum representation for the given string.
         * @throws IllegalArgumentException if unknown string.
         */
        public static DimensionType fromString(String s) throws IllegalArgumentException {
            return Arrays.stream(DimensionType.values())
                    .filter(v -> v.dimensionType.equals(s))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("unknown value: " + s));
        }

    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public UnitType getUnit() {
        return unit;
    }

    public void setUnit(UnitType unit) {
        this.unit = unit;
    }

    public List<DimensionType> getDimensions() {
        return dimensions;
    }

    public void setDimensions(List<DimensionType> dimensions) {
        this.dimensions = dimensions;
    }
}
