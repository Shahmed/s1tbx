package org.esa.nest;

import com.bc.ceres.core.NullProgressMonitor;
import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import junit.framework.TestCase;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.internal.OperatorExecutor;
import org.esa.beam.gpf.operators.standard.WriteOp;
import org.esa.beam.util.StopWatch;
import org.esa.nest.datamodel.AbstractMetadata;
import org.esa.nest.util.MemUtils;
import org.esa.nest.util.TestUtils;

import java.io.File;

/**
 * Benchmark code
 */
public abstract class Benchmark extends TestCase {
    private OperatorSpi spi;

    protected boolean skipS1 = false;

    private final File outputFile = new File("e:\\out\\output.dim");

    public Benchmark() {
        try {
            TestUtils.initTestEnvironment();
            DataSets.instance();

            spi = CreateOperatorSpi();
            GPF.getDefaultInstance().getOperatorSpiRegistry().addOperatorSpi(spi);
        } catch(Throwable t) {
            t.printStackTrace();
        }
    }

    @Override
    protected void setUp() throws Exception {

    }

    @Override
    protected void tearDown() throws Exception {

    }

    protected abstract OperatorSpi CreateOperatorSpi();

    private void process(final OperatorSpi spi, final Product product) throws Throwable {
        if(!BenchConstants.runBenchmarks) return;
        if(product == null) return;

        try {
            if(BenchConstants.numIterations > 1) {
                //warm up
                run(spi, product);
            }

            final StopWatch watch = new StopWatch();
            for(int i=0; i< BenchConstants.numIterations; ++i) {
                MemUtils.freeAllMemory();
                run(spi, product);
            }
            watch.stop();

            final String mission = AbstractMetadata.getAbstractedMetadata(product).getAttributeString(AbstractMetadata.MISSION);
            final int w = product.getSceneRasterWidth();
            final int h = product.getSceneRasterHeight();
            final long milliseconds = watch.getTimeDiff() / BenchConstants.numIterations;
            final int seconds = (int)((milliseconds / 1000.0) + 0.5);

            System.out.println(spi.getOperatorAlias() +' '+ mission +' '+ product.getProductType() +' '+w +'x'+ h
                    +" avg time: "+ StopWatch.getTimeString(milliseconds) +" ("+ seconds +" s)");
        } catch(Throwable t) {
            System.out.println("Test failed " + spi.getOperatorAlias() +' '+ product.getProductType());
            throw t;
        }
    }

    private void run(final OperatorSpi spi, final Product srcProduct) throws Throwable {
        final Operator op = spi.createOperator();
        op.setSourceProduct(srcProduct);
        setOperatorParameters(op);
        writeProduct(op);
        op.dispose();
    }

    public void writeProduct(final Operator op) {

        // get targetProduct: execute initialize()
        final Product targetProduct = op.getTargetProduct();

        final WriteOp writeOp = new WriteOp(targetProduct, outputFile, "BEAM-DIMAP");
        writeOp.setDeleteOutputOnFailure(true);
        writeOp.setWriteEntireTileRows(true);
        writeOp.setClearCacheAfterRowWrite(false);

        final OperatorExecutor executor = OperatorExecutor.create(writeOp);
        executor.execute(new NullProgressMonitor());
    }

    protected void setOperatorParameters(final Operator op) {

    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    //RS-2
    public void testPerf_RS2_Quad() throws Throwable {
        process(spi, DataSets.instance().RS2_quad_product);
    }

    //ASAR
    public void testPerf_ASAR_IMS() throws Throwable {
        process(spi, DataSets.instance().ASAR_IMS_product);
    }

    public void testPerf_ASAR_IMP() throws Throwable {
        process(spi, DataSets.instance().ASAR_IMP_product);
    }

    public void testPerf_ASAR_APP() throws Throwable {
        process(spi, DataSets.instance().ASAR_APP_product);
    }

    public void testPerf_ASAR_APS() throws Throwable {
        process(spi, DataSets.instance().ASAR_APS_product);
    }

    public void testPerf_ASAR_WSM() throws Throwable {
        process(spi, DataSets.instance().ASAR_WSM_product);
    }

    //ERS-2 CEOS
    public void testPerf_ERS2_PRI() throws Throwable {
        process(spi, DataSets.instance().ERS2_PRI_product);
    }

    public void testPerf_ERS2_SLC() throws Throwable {
        process(spi, DataSets.instance().ERS2_SLC_product);
    }

    //ERS-2
    public void testPerf_ERS2_IMP() throws Throwable {
        process(spi, DataSets.instance().ERS2_IMP_product);
    }

    public void testPerf_ERS2_IMS() throws Throwable {
        process(spi, DataSets.instance().ERS2_IMS_product);
    }

    //ALOS
    public void testPerf_ALOS_L11() throws Throwable {
        process(spi, DataSets.instance().ALOS_L11_product);
    }

    //TerraSAR-X
    public void testPerf_TSX_SSC() throws Throwable {
        process(spi, DataSets.instance().TSX_SSC_product);
    }

    public void testPerf_TSX_SSC_Quad() throws Throwable {
        process(spi, DataSets.instance().TSX_SSC_Quad_product);
    }

    //S-1
    public void testPerf_S1_GRD() throws Throwable {
        if(skipS1) return;
        process(spi, DataSets.instance().S1_GRD_product);
    }

}
