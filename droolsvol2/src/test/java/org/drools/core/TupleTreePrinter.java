package org.drools.core;

public class TupleTreePrinter {

    public static String print(TupleImpl tuple) {
        StringBuilder sb = new StringBuilder();
        printNode(tuple, sb, "", true);
        return sb.toString();
    }

    private static void printNode(TupleImpl tuple, StringBuilder sb, String prefix, boolean isLast) {
        sb.append(prefix);
        sb.append(isLast ? "└── " : "├── ");
        sb.append(label(tuple));
        sb.append('\n');

        String childPrefix = prefix + (isLast ? "    " : "│   ");

        if (tuple instanceof ObjectHandleTuple) {
            return;
        }

        if (tuple.hasRightParent()) {
            // JoinTuple: has left and right
            printNode(tuple.getLeftParent(), sb, childPrefix, false);
            printNode(tuple.getRightParent(), sb, childPrefix, true);
        }
    }

    private static String label(TupleImpl tuple) {
        NetworkNode node = tuple.getNetworkNode();
        String type;
        if (tuple instanceof ObjectHandleTuple) {
            type = "Leaf";
        } else {
            type = "Join";
        }

        String value = "";
        if (tuple instanceof ObjectHandleTuple) {
            Object obj = tuple.getObject();
            value = obj != null ? " " + obj : "";
        }

        return type + "(idx=" + node.getObjectIndex() + ", size=" + node.getSize() + ")" + value;
    }
}
