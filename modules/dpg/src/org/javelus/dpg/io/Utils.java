/*
 * Copyright (C) 2012  Tianxiao Gu. All rights reserved.
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 * 
 * Please contact Institute of Computer Software, Nanjing University, 
 * 163 Xianlin Avenue, Nanjing, Jiangsu Provience, 210046, China,
 * or visit moon.nju.edu.cn if you need additional information or have any
 * questions.
 */
package org.javelus.dpg.io;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class Utils implements Opcodes {
    public static final int CLASS = 0;
    public static final int METHOD = 1;
    public static final int FIELD = 2;

    /**
     * for output field access string
     * 
     * @param access
     * @return
     */
    public static String accessToString(int access, int flag) {
        String str = null;
        if ((access & ACC_PUBLIC) != 0) {
            str = "public ";
        } else if ((access & ACC_PRIVATE) != 0) {
            str = "private ";
        } else if ((access & ACC_PROTECTED) != 0) {
            str = "protected ";
        } else {
            str = "";
        }

        if ((access & ACC_STATIC) != 0) {
            str += "static ";
        }

        if ((access & ACC_FINAL) != 0) {
            if (flag != CLASS || (access & ACC_ENUM) == 0) {
                str += "final ";
            }
        }

        if (flag == CLASS) {
            if ((access & ACC_ANNOTATION) != 0) {
                str += "@interface ";
            } else if ((access & ACC_INTERFACE) != 0) {
                str += "interface ";
            } else if ((access & ACC_ABSTRACT) != 0) {
                str += "abstract class ";
            } else if ((access & ACC_ENUM) != 0) {
                str += "enum ";
            } else {
                str += "class ";
            }
        } else if (flag == METHOD) {
            if ((access & ACC_NATIVE) != 0) {
                str += "native ";
            }
            if ((access & ACC_SYNCHRONIZED) != 0) {
                str += "synchronized ";
            }
        } else if (flag == FIELD) {
            if ((access & ACC_VOLATILE) != 0) {
                str += "volatile ";
            }
            if ((access & ACC_TRANSIENT) != 0) {
                str += "transient ";
            }
        }

        return str;
    }

    /**
     * 
     * @param desc
     * @return
     */
    public static String extractTypeFromDescriptor(String desc) {
        if (desc.startsWith("(")) {
            desc = desc.substring(desc.lastIndexOf(")") + 1);
        }
        return Type.getType(desc).getClassName();
    }

    public static String genMethodHeader(String name, String desc) {
        StringBuffer sb = new StringBuffer();

        Type returnType = Type.getReturnType(desc);
        if (returnType == Type.VOID_TYPE) {
            sb.append("void ");
        } else {
            sb.append(returnType.getClassName());
            sb.append(" ");
        }
        sb.append(name);

        sb.append(" (");
        sb.append(genMethodParameters(desc));
        sb.append(")");
        return sb.toString();
    }

    public static String genMethodParameters(String desc) {
        StringBuffer sb = new StringBuffer();
        Type[] args = Type.getArgumentTypes(desc);
        if (args.length > 0) {
            sb.append(args[0].getClassName());
            sb.append(" p0");
            for (int i = 1; i < args.length; i++) {
                sb.append(", ");
                sb.append(args[i].getClassName());
                sb.append(" p");
                sb.append(i);
            }
        }
        return sb.toString();
    }

    /**
     * 
     * @param desc
     * @return
     */
    public static String genDefaultValueFromDescriptor(String desc) {
        switch (desc.charAt(0)) {
        case 'L':
        case '[':
            return "null";
        case 'Z':
            return "(false)";
        case 'B':
            return "(0)";
        case 'S':
            return "(0)";
        case 'I':
            return "(0)";
        case 'J':
            return "(0L)";
        case 'F':
            return "(0F)";
        case 'D':
            return "(0D)";
        case 'C':
            return "((char)0)";

        case 'V':
        default:
        }
        return null;
    }

    /**
     * transform "java/lang/Object" or "java.lang.Object" to
     * "Ljava/lang/Object;"
     * 
     * Array ..
     * 
     * @param internalName
     * @return
     */
    public static String getTypeDescriptor(String internalName) {
        if (!(internalName.startsWith("L") && internalName.endsWith(";"))) {
            return String.format("L%s;", internalName.replace('.', '/'));
        }
        return internalName;
    }

    /**
     * 
     * @param name
     * @return
     */
    public static String getInnerMostInternalName(String name) {
        String innerName = name;

        if (innerName.startsWith("[")) {
            innerName = innerName.substring(innerName.lastIndexOf("[") + 1);
        }
        if (innerName.startsWith("L") && innerName.endsWith(";")) {
            innerName = innerName.substring(1, innerName.length() - 1);
        }

        return innerName;
    }

    public static void main(String[] args) {
        System.out.println(getTypeDescriptor("java.lang.Object"));
        System.out.println(getTypeDescriptor("java/lang/Object"));
    }

}
