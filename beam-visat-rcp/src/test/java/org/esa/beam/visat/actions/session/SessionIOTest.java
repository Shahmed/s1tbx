package org.esa.beam.visat.actions.session;

import com.bc.ceres.binding.ClassFieldDescriptorFactory;
import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.ValueDescriptor;
import com.bc.ceres.binding.ValueContainer;
import com.bc.ceres.binding.dom.DefaultDomConverter;
import com.bc.ceres.binding.dom.DomConverter;
import com.bc.ceres.binding.dom.DomElement;
import com.bc.ceres.binding.dom.DomElementConverter;
import com.bc.ceres.binding.dom.Xpp3DomElement;
import com.bc.ceres.binding.dom.AbstractDomConverter;
import com.bc.ceres.core.ExtensionManager;
import com.bc.ceres.core.SingleTypeExtensionFactory;
import com.bc.ceres.core.ExtensionFactory;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerType;
import com.bc.ceres.glayer.support.ImageLayer;
import com.bc.ceres.glevel.MultiLevelSource;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamConverter;
import junit.framework.TestCase;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.glayer.GraticuleLayer;

import java.awt.Color;
import java.awt.Font;
import java.awt.Rectangle;
import java.io.File;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class SessionIOTest extends TestCase {

/*
    static {
        ExtensionManager.getInstance().register(ImageLayer.Type.class, new ExtensionFactory() {
            @Override
            public Object getExtension(Object object, Class<?> extensionType) {
                return new DomConverter() {
                    @Override
                    public Class<?> getValueType() {
                        return Map.class;
                    }

                    @Override
                    public Object convertDomToValue(DomElement parentElement, Object value) throws ConversionException,
                                                                                                   ValidationException {
                                                Map<String, Object> configuration = (Map<String, Object>) value;
                        if (configuration == null) {
                            configuration = new HashMap<String, Object>();
                        }

                        parentElement.getChild("multiLevelSourceType")
                        ExtensionManager.getInstance().get

                                                final DomConverter converter = mls.getExtension(DomConverter.class);
                        converter.convertValueToDom(mls, parentElement);
                        return null;  //To change body of implemented methods use File | Settings | File Templates.
                    }

                    @Override
                    public void convertValueToDom(Object value, DomElement parentElement) {
                        Map<String, Object> configuration = (Map<String, Object>) value;
                        MultiLevelSource mls = (MultiLevelSource) configuration.get("multiLevelSource");
                        final DomConverter converter = mls.getExtension(DomConverter.class);
                        converter.convertValueToDom(mls, parentElement);
                    }
                };
            }

            @Override
            public Class<?>[] getExtensionTypes() {
                return new Class<?>[] {DomConverter.class};
            }
        });
    }
*/

    private static interface LayerIO {

        void write(Layer layer, Writer writer, DomConverter domConverter);

        LayerMemento read(Reader reader);
    }

    public void testGetInstance() {
        assertNotNull(SessionIO.getInstance());
        assertSame(SessionIO.getInstance(), SessionIO.getInstance());
    }

    public void testIO() throws Exception {
        ExtensionManager.getInstance().register(LayerType.class, new GraticuleLayerIOFactory());

        final Session session1 = SessionTest.createTestSession();

        testSession(session1);
        final StringWriter writer = new StringWriter();
        SessionIO.getInstance().writeSession(session1, writer);
        final String xml = writer.toString();
        System.out.println("Session XML:\n" + xml);
        final StringReader reader = new StringReader(xml);
        final Session session2 = SessionIO.getInstance().readSession(reader);
        testSession(session2);
    }

    private void testSession(Session session) {
        assertEquals(Session.CURRENT_MODEL_VERSION, session.getModelVersion());

        assertEquals(2, session.getProductCount());
        testProductRef(session.getProductRef(0), 11, new File("testdata/out/DIMAP/X.dim"));
        testProductRef(session.getProductRef(1), 15, new File("testdata/out/DIMAP/Y.dim"));

        assertEquals(4, session.getViewCount());
        testViewRef(session.getViewRef(0), 0, ProductSceneView.class.getName(), new Rectangle(0, 0, 200, 100), 11, "A");
        testViewRef(session.getViewRef(1), 1, ProductSceneView.class.getName(), new Rectangle(200, 0, 200, 100), 15,
                    "C");
        testViewRef(session.getViewRef(2), 2, ProductSceneView.class.getName(), new Rectangle(0, 100, 200, 100), 11,
                    "B");
        testViewRef(session.getViewRef(3), 3, ProductSceneView.class.getName(), new Rectangle(200, 100, 200, 100), 15,
                    "D");

        assertEquals(2, session.getViewRef(3).getLayerCount());
        assertEquals("[15] D", session.getViewRef(3).getLayerRef(0).name);
        final Session.LayerRef graticuleLayerRef = session.getViewRef(3).getLayerRef(1);
        assertEquals("Graticule", graticuleLayerRef.name);
        assertNotNull(graticuleLayerRef.configuration);
        assertEquals(3, graticuleLayerRef.configuration.getChildCount());
    }

    private void testProductRef(Session.ProductRef productRef, int expectedId, File expectedFile) {
        assertEquals(expectedId, productRef.id);
        assertEquals(expectedFile, productRef.file);
    }

    private void testViewRef(Session.ViewRef viewRef, int expectedId, String expectedType, Rectangle expectedBounds,
                             int expectedProductId, String expectedProductNodeName) {
        assertEquals(expectedId, viewRef.id);
        assertEquals(expectedType, viewRef.type);
        assertEquals(expectedBounds, viewRef.bounds);
        assertEquals(expectedProductId, viewRef.productId);
        assertEquals(expectedProductNodeName, viewRef.productNodeName);
    }

    static class GraticuleLayerIOFactory extends SingleTypeExtensionFactory<LayerType, LayerIO> {

        private GraticuleLayerIOFactory() {
            super(LayerIO.class, GraticuleLayerIO.class);
        }

        @Override
        protected LayerIO getExtensionImpl(LayerType layerType, Class<LayerIO> extensionType) throws Throwable {
            return new GraticuleLayerIO();
        }
    }

    static class GraticuleLayerIO implements LayerIO {

        private volatile XStream xs;

        @Override
        public void write(Layer layer, Writer writer, DomConverter domConverter) {
            initIO();

            final Xpp3DomElement configuration = Xpp3DomElement.createDomElement("configuration");
            domConverter.convertValueToDom(layer, configuration);

            final LayerMemento memento = new LayerMemento(layer.getLayerType().getName(), configuration);
            xs.toXML(memento, writer);
        }

        @Override
        public LayerMemento read(Reader reader) {
            initIO();

            final Object obj = xs.fromXML(reader);
            assertTrue(obj instanceof LayerMemento);

            return (LayerMemento) obj;
        }

        private void initIO() {
            if (xs == null) {
                synchronized (this) {
                    if (xs == null) {
                        xs = new XStream();
                        xs.processAnnotations(LayerMemento.class);
                        xs.alias("configuration", DomElement.class, Xpp3DomElement.class);
                        xs.useAttributeFor(LayerMemento.class, "typeName");
                    }
                }
            }
        }
    }

    @XStreamAlias("layer")
    static class LayerMemento {

        @XStreamAlias("type")
        private String typeName;

        @XStreamConverter(DomElementConverter.class)
        private DomElement configuration;

        LayerMemento(String typeName, DomElement configuration) {
            this.typeName = typeName;
            this.configuration = configuration;
        }

        DomElement getConfiguration() {
            return configuration;
        }
    }

    static class RestaurantLayer {
        boolean visible;
        double transparency;

        Color bgPaint;
        Color fgPaint;
        boolean showBorder;
        Font labelFont;
    }

    static class RestaurantLayerIO {
        
        private volatile XStream xs;
        private volatile DomConverter dc;

        public void write(RestaurantLayer layer, Writer writer) {
            initIO();

            final Xpp3DomElement configuration = Xpp3DomElement.createDomElement("configuration");
            dc.convertValueToDom(layer, configuration);

            final Memento memento = new Memento("restaurants", configuration);
            xs.toXML(memento, writer);
        }

        public Memento readMemento(Reader reader) {
            initIO();

            final Object obj = xs.fromXML(reader);
            assertTrue(obj instanceof Memento);

            return (Memento) obj;
        }

        private void initIO() {
            if (xs == null) {
                synchronized (this) {
                    if (xs == null) {
                        xs = new XStream();
                        xs.processAnnotations(Memento.class);
                        xs.alias("configuration", DomElement.class, Xpp3DomElement.class);
                        xs.useAttributeFor(Memento.class, "typeName");
                        final ClassFieldDescriptorFactory factory = new ClassFieldDescriptorFactory() {
                            @Override
                            public ValueDescriptor createValueDescriptor(Field field) {
                                return new ValueDescriptor(field.getName(), field.getType());
                            }
                        };
                        dc = new DefaultDomConverter(RestaurantLayer.class, factory);
                    }
                }
            }
        }

        @XStreamAlias("layer")
        static class Memento {
            @XStreamAlias("type")
            private final String typeName;

            @XStreamConverter(DomElementConverter.class)
            private final DomElement configuration;

            public DomElement getConfiguration() {
                return configuration;
            }

            private Memento(String typeName, DomElement configuration) {
                this.typeName = typeName;
                this.configuration = configuration;
            }
        }
    }

    public void testRestaurantLayerIO() throws ValidationException, ConversionException {
        // create example layer
        final RestaurantLayer layer1 = new RestaurantLayer();
        layer1.visible = true;
        layer1.transparency = 0.5;
        layer1.bgPaint = Color.BLACK;
        layer1.fgPaint = Color.GREEN;
        layer1.showBorder = true;
        layer1.labelFont = new Font("helvetica", Font.ITALIC, 11);

        final ClassFieldDescriptorFactory factory = new ClassFieldDescriptorFactory() {
            @Override
            public ValueDescriptor createValueDescriptor(Field field) {
                return new ValueDescriptor(field.getName(), field.getType());
            }
        };
        final DefaultDomConverter domConverter = new DefaultDomConverter(RestaurantLayer.class, factory);

        // write layer to string
        final RestaurantLayerIO layerIO = new RestaurantLayerIO();
        final StringWriter writer = new StringWriter();
        layerIO.write(layer1, writer);
        final String xml = writer.getBuffer().toString();
        System.out.println(xml);

        final StringReader reader = new StringReader(xml);
        RestaurantLayerIO.Memento memento = layerIO.readMemento(reader);
        // create layer from layer representation
        final RestaurantLayer layer2 = new RestaurantLayer();
        domConverter.convertDomToValue(memento.getConfiguration(), layer2);

        // test equality of original and restored layers
        assertEquals(true, layer2.visible);
        assertEquals(0.5, layer2.transparency, 0.0);
        assertEquals(Color.BLACK, layer2.bgPaint);
        assertEquals(Color.GREEN, layer2.fgPaint);
        assertEquals(true, layer2.showBorder);
        assertEquals(new Font("helvetica", Font.ITALIC, 11), layer2.labelFont);
    }
}
