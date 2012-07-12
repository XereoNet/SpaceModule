package me.neatmonster.spacemodule.utilities;


import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.core.util.HierarchicalStreams;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.mapper.CannotResolveClassException;
import com.thoughtworks.xstream.mapper.Mapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class XMLListConverter implements Converter {

    private Mapper mapper;


    public XMLListConverter(Mapper mapper) {
        this.mapper = mapper;
    }


    @Override
    public void marshal(Object obj, HierarchicalStreamWriter writer, MarshallingContext context) {
        List list = (List)obj;
        for(Object o : list) {
            context.convertAnother(o);
        }
    }

    @Override
    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
        Map<String, List> map = new HashMap<String, List>(); //Preserve the name of the list in a mapping to said list.
        List list = new ArrayList();
        map.put(reader.getNodeName(), list);

        while(reader.hasMoreChildren()) {
            reader.moveDown();
            try {
                Object value = context.convertAnother(reader.getValue(), HierarchicalStreams.readClassType(reader, mapper));
                list.add(value);
            } catch(CannotResolveClassException e) {
                list.add(context.convertAnother(reader.getValue(), Map.class));
            }
            reader.moveUp();
        }

        return map;
    }

    @Override
    public boolean canConvert(Class aClass) {
        return List.class.isAssignableFrom(aClass);
    }
}
