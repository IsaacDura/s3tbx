package org.esa.s3tbx.c2rcc;

import org.esa.s3tbx.c2rcc.meris.C2rccMerisOperator;
import org.esa.s3tbx.c2rcc.modis.C2rccModisOperator;
import org.esa.s3tbx.c2rcc.msi.C2rccMsiOperator;
import org.esa.s3tbx.c2rcc.olci.C2rccOlciOperator;
import org.esa.s3tbx.c2rcc.seawifs.C2rccSeaWiFSOperator;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.core.util.converters.BooleanExpressionConverter;

import static org.esa.snap.core.util.StringUtils.*;

/**
 * The Case 2 Regional / CoastColour Operator for MERIS, MODIS, SeaWiFS, and VIIRS.
 * <p/>
 * Computes AC-reflectances and IOPs from MERIS, MODIS, SeaWiFS, and VIIRS L1b data products using
 * an neural-network approach.
 *
 * @author Norman Fomferra
 * @author Sabine Embacher
 */
@OperatorMetadata(alias = "c2rcc", version = "0.9.1",
            authors = "Roland Doerffer, Norman Fomferra, Sabine Embacher (Brockmann Consult)",
            category = "Optical Processing/Thematic Water Processing",
            copyright = "Copyright (C) 2015 by Brockmann Consult",
            description = "Performs atmospheric correction and IOP retrieval on OLCI, MSI, MERIS, MODIS or SeaWiFS L1 product.")
public class C2rccOperator extends Operator {
    /*
        c2rcc ops have been removed from Graph Builder. In the layer xml they are disabled
        see https://senbox.atlassian.net/browse/SNAP-395
    */


    @SourceProduct(description = "OLCI, MSI, MERIS, MODIS or SeaWiFS L1 product")
    private Product sourceProduct;

    @SourceProduct(description = "The first product providing ozone values for ozone interpolation. " +
                                 "Use either this in combination with other start- and end-products (tomsomiEndProduct, " +
                                 "ncepStartProduct, ncepEndProduct) or atmosphericAuxdataPath to use ozone and air pressure " +
                                 "aux data for calculations.",
                optional = true,
                label = "Ozone interpolation start product (TOMSOMI)")
    private Product tomsomiStartProduct;

    @SourceProduct(description = "The second product providing ozone values for ozone interpolation. " +
                                 "Use either this in combination with other start- and end-products (tomsomiStartProduct, " +
                                 "ncepStartProduct, ncepEndProduct) or atmosphericAuxdataPath to use ozone and air pressure " +
                                 "aux data for calculations.",
                optional = true,
                label = "Ozone interpolation end product (TOMSOMI)")
    private Product tomsomiEndProduct;

    @SourceProduct(description = "The first product providing air pressure values for pressure interpolation. " +
                                 "Use either this in combination with other start- and end-products (tomsomiStartProduct, " +
                                 "tomsomiEndProduct, ncepEndProduct) or atmosphericAuxdataPath to use ozone and air pressure " +
                                 "aux data for calculations.",
                optional = true,
                label = "Air pressure interpolation start product (NCEP)")
    private Product ncepStartProduct;

    @SourceProduct(description = "The second product providing air pressure values for pressure interpolation. " +
                                 "Use either this in combination with other start- and end-products (tomsomiStartProduct, " +
                                 "tomsomiEndProduct, ncepStartProduct) or atmosphericAuxdataPath to use ozone and air pressure " +
                                 "aux data for calculations.",
                optional = true,
                label = "Air pressure interpolation end product (NCEP)")
    private Product ncepEndProduct;

    @TargetProduct
    private Product targetProduct;

    @Parameter(valueSet = {"", "olci", "msi", "meris", "modis", "seawifs"})
    private String sensorName;

    @Parameter(label = "Valid-pixel expression", converter = BooleanExpressionConverter.class,
                description = "If not specified a sensor specific default expression will be used.")
    private String validPixelExpression;

    @Parameter(defaultValue = "35.0", unit = "DU", interval = "(0, 100)")
    private double salinity;

    @Parameter(defaultValue = "15.0", unit = "C", interval = "(-50, 50)")
    private double temperature;

    @Parameter(defaultValue = "330", unit = "DU", interval = "(0, 1000)")
    private double ozone;

    @Parameter(defaultValue = "1000", unit = "hPa", interval = "(0, 2000)", label = "Air Pressure")
    private double press;

    @Parameter(description = "Path to the atmospheric auxiliary data directory. Use either this or tomsomiStartProduct, " +
                             "tomsomiEndProduct, ncepStartProduct and ncepEndProduct to use ozone and air pressure aux data " +
                             "for calculations. If the auxiliary data needed for interpolation not available in this " +
                             "path, the data will automatically downloaded.")
    private String atmosphericAuxDataPath;

    @Parameter(defaultValue = "false", label = "Output top-of-standard-atmosphere (TOSA) reflectances")
    private boolean outputRtosa;

    @Parameter(defaultValue = "false", label = "Use default solar flux (in case of MERIS sensor)")
    private boolean useDefaultSolarFlux;

    @Parameter(defaultValue = "false", description =
                "If selected, the ecmwf auxiliary data (ozon, air pressure) of the source product is used",
                label = "Use ECMWF aux data of source product (in case of MERIS sensor)")
    private boolean useEcmwfAuxData;

    @Override
    public void initialize() throws OperatorException {
        if (isMeris(sourceProduct)) {
            C2rccMerisOperator c2rccMerisOperator = new C2rccMerisOperator();
            c2rccMerisOperator.setParameterDefaultValues();
            c2rccMerisOperator.setUseDefaultSolarFlux(useDefaultSolarFlux);
            c2rccMerisOperator.setUseEcmwfAuxData(useEcmwfAuxData);
            configure(c2rccMerisOperator);
            targetProduct = setSourceAndGetTarget(c2rccMerisOperator);
        } else if (isModis(sourceProduct)) {
            C2rccModisOperator c2rccModisOperator = new C2rccModisOperator();
            c2rccModisOperator.setParameterDefaultValues();
            configure(c2rccModisOperator);
            targetProduct = setSourceAndGetTarget(c2rccModisOperator);
        } else if (isSeawifs(sourceProduct)) {
            C2rccSeaWiFSOperator c2rccSeaWiFSOperator = new C2rccSeaWiFSOperator();
            c2rccSeaWiFSOperator.setParameterDefaultValues();
            configure(c2rccSeaWiFSOperator);
            targetProduct = setSourceAndGetTarget(c2rccSeaWiFSOperator);
        } else if (isNotNullAndNotEmpty(sensorName) && "viirs".equalsIgnoreCase(sensorName)) {
            throw new OperatorException("The VIIRS operator is currently not implemented.");
        } else if (isOlci(sourceProduct)) {
            C2rccOlciOperator c2rccOlciOperator = new C2rccOlciOperator();
            c2rccOlciOperator.setParameterDefaultValues();
            c2rccOlciOperator.setUseEcmwfAuxData(useEcmwfAuxData);
            configure(c2rccOlciOperator);
            targetProduct = setSourceAndGetTarget(c2rccOlciOperator);
        } else if (isMsi(sourceProduct)) {
            C2rccMsiOperator c2rccMsiOperator = new C2rccMsiOperator();
            c2rccMsiOperator.setParameterDefaultValues();
            c2rccMsiOperator.setUseEcmwfAuxData(useEcmwfAuxData);
            configure(c2rccMsiOperator);
            targetProduct = setSourceAndGetTarget(c2rccMsiOperator);
        } else {
            throw new OperatorException("Illegal source product.");
        }

    }

    private Product setSourceAndGetTarget(Operator operator) {
        operator.setSourceProduct(sourceProduct);
        return operator.getTargetProduct();
    }

    private void configure(C2rccConfigurable c2rConfigOp) {
        c2rConfigOp.setTomsomiStartProduct(tomsomiStartProduct);
        c2rConfigOp.setTomsomiEndProduct(tomsomiEndProduct);
        c2rConfigOp.setNcepStartProduct(ncepStartProduct);
        c2rConfigOp.setNcepEndProduct(ncepEndProduct);
        if (StringUtils.isNotNullAndNotEmpty(validPixelExpression)) {
            c2rConfigOp.setValidPixelExpression(validPixelExpression);
        }
        c2rConfigOp.setSalinity(salinity);
        c2rConfigOp.setTemperature(temperature);
        c2rConfigOp.setOzone(ozone);
        c2rConfigOp.setPress(press);
        c2rConfigOp.setAtmosphericAuxDataPath(atmosphericAuxDataPath);
        c2rConfigOp.setOutputRtosa(outputRtosa);
    }


    private boolean isMeris(Product product) {
        if (isNotNullAndNotEmpty(sensorName)) {
            return "meris".equalsIgnoreCase(sensorName);
        } else {
            return C2rccMerisOperator.isValidInput(product);
        }
    }

    private boolean isModis(Product product) {
        if (isNotNullAndNotEmpty(sensorName)) {
            return "modis".equalsIgnoreCase(sensorName);
        } else {
            return C2rccModisOperator.isValidInput(product);
        }
    }

    private boolean isSeawifs(Product product) {
        if (isNotNullAndNotEmpty(sensorName)) {
            return "seawifs".equalsIgnoreCase(sensorName);
        } else {
            return C2rccSeaWiFSOperator.isValidInput(product);
        }
    }

    private boolean isOlci(Product product) {
        if (isNotNullAndNotEmpty(sensorName)) {
            return "olci".equalsIgnoreCase(sensorName);
        } else {
            return C2rccOlciOperator.isValidInput(product);
        }
    }

    private boolean isMsi(Product product) {
        if (isNotNullAndNotEmpty(sensorName)) {
            return "msi".equalsIgnoreCase(sensorName);
        } else {
            return C2rccMsiOperator.isValidInput(product);
        }
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(C2rccOperator.class);
        }
    }
}
