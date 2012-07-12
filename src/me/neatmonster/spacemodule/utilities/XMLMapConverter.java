package me.neatmonster.spacemodule.utilities;


import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.core.util.HierarchicalStreams;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.mapper.CannotResolveClassException;
import com.thoughtworks.xstream.mapper.Mapper;

import java.util.HashMap;
import java.util.Map;

public class XMLMapConverter implements Converter {

    private Mapper mapper;

    public XMLMapConverter(Mapper mapper) {
        this.mapper = mapper;
    }


    @Override
    public void marshal(Object obj, HierarchicalStreamWriter writer, MarshallingContext context) {
        Map map = (Map)obj;
        for(Object o : map.entrySet()) {
            Map.Entry e = (Map.Entry)o;

            writer.startNode(e.getKey().toString());
            context.convertAnother(e.getValue());
            writer.endNode();
        }
    }

    @Override
    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
        Map<String, Object> map = new HashMap<String, Object>();
        while(reader.hasMoreChildren()) {
            reader.moveDown();
            try {
                Object value = context.convertAnother(reader.getValue(), HierarchicalStreams.readClassType(reader, mapper));
                map.put(reader.getNodeName(), value);
            } catch(CannotResolveClassException e) {
                map.put(reader.getNodeName(), reader.getValue());
            }
            reader.moveUp();
        }
        return map;
    }

    @Override
    public boolean canConvert(Class aClass) {
        return Map.class.isAssignableFrom(aClass);
    }
}
