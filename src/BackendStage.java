import java.util.*;

import org.specs.comp.ollir.*;

import pt.up.fe.comp.jmm.jasmin.JasminBackend;
import pt.up.fe.comp.jmm.jasmin.JasminResult;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.specs.util.SpecsIo;

/**
 * Copyright 2021 SPeCS.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License. under the License.
 */

public class BackendStage implements JasminBackend {
    StringBuilder jasminString = new StringBuilder();
    ArrayList<String> imports;
    int CurrentLabel = 0;
    int StackSize = 0;
    int MaxStackSize = 0;
    String superClass;
    @Override
    public JasminResult toJasmin(OllirResult ollirResult) {
        ClassUnit ollirClass = ollirResult.getOllirClass();
        try {

            // Example of what you can do with the OLLIR class
            ollirClass.checkMethodLabels(); // check the use of labels in the OLLIR loaded
            ollirClass.buildCFGs(); // build the CFG of each method
            ollirClass.outputCFGs(); // output to .dot files the CFGs, one per method
            ollirClass.buildVarTables(); // build the table of variables for each method
            ollirClass.show(); // print to console main information about the input OLLIR

            // Convert the OLLIR to a String containing the equivalent Jasmin code
            imports = ollirClass.getImports();
            jasminString.append(".class ");
            String auxclass = "";
            if(ollirClass.getClassAccessModifier() != AccessModifiers.DEFAULT) auxclass += ollirClass.getClassAccessModifier().name().toLowerCase() + " ";
            if(ollirClass.isStaticClass()) auxclass += "static ";
            if(ollirClass.isFinalClass()) auxclass += "final ";
            auxclass += ollirClass.getClassName() + '\n';
            jasminString.append(auxclass);
            if(ollirClass.getSuperClass() != null){
                superClass = ollirClass.getSuperClass();
                jasminString.append(".super " + superClass + "\n");
            }
            else jasminString.append(".super java/lang/Object\n");
            for(Field field : ollirClass.getFields()){
                String fieldAccessModifier = field.getFieldAccessModifier() == AccessModifiers.DEFAULT ? "" : " " + field.getFieldAccessModifier().name().toLowerCase() + " ";
                jasminString.append(".field" + fieldAccessModifier + "'" + field.getFieldName() + "'" + " " + generateType(field.getFieldType()));
                if (field.isInitialized()) {
                    jasminString.append(" " + field.getInitialValue());
                }
                jasminString.append("\n");
            }
            jasminString.append(parseMethods(ollirClass.getMethods()));
            String jasminCode = jasminString.toString(); // Convert node ...
            // More reports from this stage
            List<Report> reports = new ArrayList<>();
            System.out.println(jasminCode);
            return new JasminResult(ollirResult, jasminCode, reports);
            //return null;

        } catch (OllirErrorException e) {
            return new JasminResult(ollirClass.getClassName(), null,
                    Arrays.asList(Report.newError(Stage.GENERATION, -1, -1, "Exception during Jasmin generation", e)));
        }

    }

    private String parseMethods(ArrayList<Method> methods){
        StringBuilder result = new StringBuilder();
        for(Method method : methods){
            String auxmethod = "";
            String auxheader = "";
            StackSize = 0;
            MaxStackSize = 0;
            auxheader += ".method ";
            if (method.isConstructMethod()){
                //auxheader += "public <init>()V\n\taload_0\n\tinvokespecial java/lang/Object.<init>()V\n\treturn\n";
                if(superClass != null) auxheader += "public <init>()V\n\taload_0\n\tinvokespecial " + superClass + ".<init>()V\n\treturn\n";
                else auxheader += "public <init>()V\n\taload_0\n\tinvokespecial java/lang/Object.<init>()V\n\treturn\n";
            }
            else {
                if (method.getMethodAccessModifier() != AccessModifiers.DEFAULT)
                    auxheader += method.getMethodAccessModifier().name().toLowerCase() + " ";
                if (method.isStaticMethod()) auxheader += "static ";
                if (method.isFinalMethod()) auxheader += "final ";
                auxheader += method.getMethodName();
                auxheader += "(";
                for (Element param : method.getParams()) {
                    auxheader += generateType(param.getType());
                }
                auxheader += ")";
                auxheader += generateType(method.getReturnType()) + "\n";
                //auxmethod += "\t.limit stack 100\n";
                auxheader += "\t.limit locals " + (method.getVarTable().size()+1) + "\n";
                for (Instruction instruction : method.getInstructions()) {
                    auxmethod += addLabel(method, instruction);
                    switch (instruction.getInstType()) {
                        case ASSIGN:
                            AssignInstruction assinstr = (AssignInstruction) instruction;
                            auxmethod += parseAssInstr(method, assinstr);
                            break;
                        case CALL:
                            CallInstruction callinstr = (CallInstruction) instruction;
                            auxmethod += parseCallInstr(method, callinstr, true);
                            break;
                        case GOTO:
                            GotoInstruction gotoinstr = (GotoInstruction) instruction;
                            auxmethod+= "\tgoto " + gotoinstr.getLabel() + "\n";
                            break;
                        case BRANCH:
                            CondBranchInstruction branchinstr = (CondBranchInstruction) instruction;
                            if(branchinstr.getCondOperation().getOpType().name().equals("NOTB")){
                                if(branchinstr.getLeftOperand() != null){
                                    if(branchinstr.getLeftOperand().isLiteral()) {
                                        if(branchinstr.getLeftOperand().getType().getTypeOfElement() == ElementType.INT32 || branchinstr.getLeftOperand().getType().getTypeOfElement() == ElementType.BOOLEAN){
                                            auxmethod+="\t" + selectConstType(((LiteralElement) branchinstr.getLeftOperand()).getLiteral()) + "\n";
                                        }
                                    }
                                    else{
                                        String register = selectRegister(method, ((Operand) branchinstr.getLeftOperand()).getName());
                                        if (branchinstr.getLeftOperand().getType().getTypeOfElement() == ElementType.INT32 || branchinstr.getLeftOperand().getType().getTypeOfElement() == ElementType.BOOLEAN) {
                                            auxmethod += "\tiload" + register + "\n";
                                            StackSize++;
                                            MaxStackSize = Math.max(StackSize, MaxStackSize);
                                        } else {
                                            auxmethod += "\taload" + register + "\n";
                                            StackSize++;
                                            MaxStackSize = Math.max(StackSize, MaxStackSize);
                                        }
                                    }
                                }
                                auxmethod+="\tifeq " + branchinstr.getLabel() + "\n";
                                StackSize--;
                                StackSize = Math.max(StackSize, 0);
                            }
                            else {
                                boolean zeroonright = false;
                                if((branchinstr.getLeftOperand() != null && branchinstr.getLeftOperand().isLiteral() && ((LiteralElement) branchinstr.getLeftOperand()).getLiteral().equals("0")) ||
                                        (zeroonright = (branchinstr.getRightOperand() != null && branchinstr.getRightOperand().isLiteral() && ((LiteralElement) branchinstr.getRightOperand()).getLiteral().equals("0")))){
                                    if(zeroonright){
                                        String register = selectRegister(method, ((Operand) branchinstr.getLeftOperand()).getName());
                                        if (branchinstr.getLeftOperand().getType().getTypeOfElement() == ElementType.INT32 || branchinstr.getLeftOperand().getType().getTypeOfElement() == ElementType.BOOLEAN) {
                                            auxmethod += "\tiload" + register + "\n";
                                            StackSize++;
                                            MaxStackSize = Math.max(StackSize, MaxStackSize);
                                        }
                                    }
                                    else{
                                        if (branchinstr.getRightOperand().isLiteral()) {
                                            if (branchinstr.getRightOperand().getType().getTypeOfElement() == ElementType.INT32 || branchinstr.getRightOperand().getType().getTypeOfElement() == ElementType.BOOLEAN) {
                                                auxmethod += "\t" + selectConstType(((LiteralElement) branchinstr.getRightOperand()).getLiteral()) + "\n";
                                            }
                                        } else{
                                            String register = selectRegister(method, ((Operand) branchinstr.getRightOperand()).getName());
                                            if (branchinstr.getRightOperand().getType().getTypeOfElement() == ElementType.INT32 || branchinstr.getRightOperand().getType().getTypeOfElement() == ElementType.BOOLEAN) {
                                                auxmethod += "\tiload" + register + "\n";
                                                StackSize++;
                                                MaxStackSize = Math.max(StackSize, MaxStackSize);
                                            }
                                        }
                                    }
                                    switch (branchinstr.getCondOperation().getOpType()){
                                            case EQ:
                                                auxmethod += "\tifeq " + branchinstr.getLabel() + "\n";
                                                break;
                                            case LTH:
                                                if(zeroonright){
                                                    auxmethod += "\tiflt " + branchinstr.getLabel() + "\n";
                                                }
                                                else{
                                                    auxmethod += "\tifgt " + branchinstr.getLabel() + "\n";
                                                }
                                                break;
                                            case GTE:
                                                if(zeroonright){
                                                    auxmethod += "\tifge " + branchinstr.getLabel() + "\n";
                                                }
                                                else{
                                                    auxmethod += "\tifle " + branchinstr.getLabel() + "\n";
                                                }
                                                break;
                                    }
                                }
                                else {
                                    if (branchinstr.getLeftOperand() != null) {
                                        if (branchinstr.getLeftOperand().isLiteral()) {
                                            if (branchinstr.getLeftOperand().getType().getTypeOfElement() == ElementType.INT32 || branchinstr.getLeftOperand().getType().getTypeOfElement() == ElementType.BOOLEAN) {
                                                auxmethod += "\t" + selectConstType(((LiteralElement) branchinstr.getLeftOperand()).getLiteral()) + "\n";
                                            }
                                        } else {
                                            String register = selectRegister(method, ((Operand) branchinstr.getLeftOperand()).getName());
                                            if (branchinstr.getLeftOperand().getType().getTypeOfElement() == ElementType.INT32 || branchinstr.getLeftOperand().getType().getTypeOfElement() == ElementType.BOOLEAN) {
                                                auxmethod += "\tiload" + register + "\n";
                                                StackSize++;
                                                MaxStackSize = Math.max(StackSize, MaxStackSize);
                                            } else {
                                                auxmethod += "\taload" + register + "\n";
                                                StackSize++;
                                                MaxStackSize = Math.max(StackSize, MaxStackSize);
                                            }
                                        }
                                    }
                                    if (branchinstr.getRightOperand() != null) {
                                        if (branchinstr.getRightOperand().isLiteral()) {
                                            if (branchinstr.getRightOperand().getType().getTypeOfElement() == ElementType.INT32 || branchinstr.getRightOperand().getType().getTypeOfElement() == ElementType.BOOLEAN) {
                                                auxmethod += "\t" + selectConstType(((LiteralElement) branchinstr.getRightOperand()).getLiteral()) + "\n";
                                            }
                                        } else {
                                            String register = selectRegister(method, ((Operand) branchinstr.getRightOperand()).getName());
                                            if (branchinstr.getRightOperand().getType().getTypeOfElement() == ElementType.INT32 || branchinstr.getRightOperand().getType().getTypeOfElement() == ElementType.BOOLEAN) {
                                                auxmethod += "\tiload" + register + "\n";
                                                StackSize++;
                                                MaxStackSize = Math.max(StackSize, MaxStackSize);
                                            } else {
                                                auxmethod += "\taload" + register + "\n";
                                                StackSize++;
                                                MaxStackSize = Math.max(StackSize, MaxStackSize);
                                            }
                                        }
                                    }
                                    if (branchinstr.getCondOperation().getOpType().name().equals("GTE")) {
                                        auxmethod += "\tif_icmpge " + branchinstr.getLabel() + "\n";
                                        StackSize--;
                                        StackSize = Math.max(StackSize, 0);
                                    } else if (branchinstr.getCondOperation().getOpType().name().equals("EQ")) {
                                        auxmethod += "\tif_icmpeq " + branchinstr.getLabel() + "\n";
                                        StackSize--;
                                        StackSize = Math.max(StackSize, 0);
                                    } else if (branchinstr.getCondOperation().getOpType().name().equals("LTH")) {
                                        auxmethod += "\tif_icmplt " + branchinstr.getLabel() + "\n";
                                        StackSize--;
                                        StackSize = Math.max(StackSize, 0);
                                    }
                                }
                            }
                            break;
                        case RETURN:
                            ReturnInstruction returninstr = (ReturnInstruction) instruction;
                            if(returninstr.getOperand() == null){
                                auxmethod+="\treturn\n";
                            }
                            else{
                                if (returninstr.getOperand().isLiteral()) {
                                    auxmethod += "\t" + selectConstType(((LiteralElement) returninstr.getOperand()).getLiteral()) + "\n";
                                    if (returninstr.getOperand().getType().getTypeOfElement() == ElementType.INT32 || returninstr.getOperand().getType().getTypeOfElement() == ElementType.BOOLEAN) {
                                        auxmethod += "\tireturn\n";
                                        StackSize-=2;
                                        StackSize = Math.max(StackSize, 0);
                                    } else {
                                        auxmethod += "\tareturn\n";
                                        StackSize-=2;
                                        StackSize = Math.max(StackSize, 0);
                                    }
                                } else {
                                    String register = selectRegister(method, ((Operand) returninstr.getOperand()).getName());
                                    if (returninstr.getOperand().getType().getTypeOfElement() == ElementType.INT32 || returninstr.getOperand().getType().getTypeOfElement() == ElementType.BOOLEAN) {
                                        auxmethod += "\tiload" + register + "\n";
                                        StackSize++;
                                        MaxStackSize = Math.max(StackSize, MaxStackSize);
                                        auxmethod += "\tireturn\n";
                                    } else {
                                        auxmethod += "\taload" + register + "\n";
                                        StackSize++;
                                        MaxStackSize = Math.max(StackSize, MaxStackSize);
                                        auxmethod += "\tareturn\n";
                                        StackSize--;
                                        StackSize = Math.max(StackSize, 0);
                                    }
                                }
                            }
                            break;
                        case PUTFIELD:
                            PutFieldInstruction putfieldinstr = (PutFieldInstruction) instruction;
                            Element value = putfieldinstr.getThirdOperand();
                            String valtype = "V";
                            valtype = generateType(value.getType());
                            auxmethod += "\taload" + selectRegister(method, ((Operand)putfieldinstr.getFirstOperand()).getName()) + "\n";
                            StackSize++;
                            MaxStackSize = Math.max(StackSize, MaxStackSize);
                            if(value.isLiteral()){
                                auxmethod += "\t" + selectConstType(((LiteralElement)value).getLiteral()) + "\n";
                            }
                            else {
                                if (value.getType().getTypeOfElement() == ElementType.INT32 || value.getType().getTypeOfElement() == ElementType.BOOLEAN) {
                                    auxmethod += "\tiload" + selectRegister(method, ((Operand) value).getName()) + "\n";
                                    StackSize++;
                                    MaxStackSize = Math.max(StackSize, MaxStackSize);
                                } else {
                                    auxmethod += "\taload" + selectRegister(method, ((Operand) value).getName()) + "\n";
                                    StackSize++;
                                    MaxStackSize = Math.max(StackSize, MaxStackSize);
                                }
                            }
                            auxmethod += "\tputfield " + getFullImportName(((ClassType)putfieldinstr.getFirstOperand().getType()).getName()) + "/" + ((Operand)putfieldinstr.getSecondOperand()).getName() + " " + valtype + "\n";
                            StackSize-=2;
                            StackSize = Math.max(StackSize, 0);
                            break;
                        case GETFIELD:
                            GetFieldInstruction getfieldinstr = (GetFieldInstruction) instruction;
                            auxmethod += parseGetFieldInstr(method, getfieldinstr);
                            break;
                    }
                }
                if (method.getReturnType().getTypeOfElement() == ElementType.VOID) {
                    auxmethod += "\treturn\n";
                }
                auxheader += "\t.limit stack " + MaxStackSize + "\n";
            }

            result.append(auxheader);
            result.append(auxmethod);
            result.append(".end method\n");
        }
        result.deleteCharAt(result.length()-1);

        return result.toString();
    }

    private String addLabel(Method method, Instruction instruction) {
        String result = "";
        Iterator it = method.getLabels().entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();
            if (instruction == pair.getValue()){
                result += pair.getKey().toString() + ":\n";
            }
        }
        return result;
    }

    private String parseAssInstr(Method method, AssignInstruction instr){
        String aux = "";
        String destreg = selectRegister(method, ((Operand)instr.getDest()).getName());
        String auxlabel = "";
        /*String auxoptimize = "";
        String auxnonop = "";*/
        boolean opflag = false;
        if(instr.getRhs() instanceof SingleOpInstruction){
            Element value = ((SingleOpInstruction) instr.getRhs()).getSingleOperand();
            if(instr.getDest() instanceof ArrayOperand && !instr.getDest().isLiteral()){
                aux+="\taload" + destreg + "\n";
                StackSize++;
                MaxStackSize = Math.max(StackSize, MaxStackSize);
                Element auxel = ((ArrayOperand)instr.getDest()).getIndexOperands().get(0);
                if(auxel.isLiteral()){
                   aux += "\t" + selectConstType(((LiteralElement)auxel).getLiteral()) + "\n";
                }
                else{
                    Operand auxop = (Operand) auxel;
                    if(auxop.getType().toString().equals("INT32") || auxop.getType().toString().equals("BOOLEAN")){
                        aux += "\tiload" + selectRegister(method, auxop.getName()) + '\n';
                        StackSize++;
                        MaxStackSize = Math.max(StackSize, MaxStackSize);
                    }
                    else{
                        aux += "\taload" + selectRegister(method, auxop.getName()) + '\n';
                        StackSize++;
                        MaxStackSize = Math.max(StackSize, MaxStackSize);
                    }
                }
                if(value.isLiteral()){
                    aux += "\t" + selectConstType(((LiteralElement)value).getLiteral()) + "\n";
                }
                else {
                    Operand variab = (Operand) value;
                    String register = selectRegister(method, variab.getName());
                    if (variab.getType().toString().equals("INT32") || variab.getType().toString().equals("BOOLEAN")) {
                        aux += "\tiload" + register + '\n';
                        StackSize++;
                        MaxStackSize = Math.max(StackSize, MaxStackSize);
                    } else {
                        aux += "\taload" + register + '\n';
                        StackSize++;
                        MaxStackSize = Math.max(StackSize, MaxStackSize);
                    }
                }
                aux += "\tiastore\n";
                StackSize -= 3;
                StackSize = Math.max(StackSize, 0);
            }
            else if(value.isLiteral()){
                aux += "\t" + selectConstType(((LiteralElement)value).getLiteral()) + '\n';
            }
            else if(value instanceof ArrayOperand){
                aux+= "\taload" + selectRegister(method, ((ArrayOperand)value).getName()) + "\n";
                StackSize++;
                MaxStackSize = Math.max(StackSize, MaxStackSize);
                aux+= "\tiload" + selectRegister(method, ((Operand)(((ArrayOperand)value).getIndexOperands().get(0))).getName()) + "\n";
                StackSize++;
                MaxStackSize = Math.max(StackSize, MaxStackSize);
                aux+= "\tiaload\n";
                StackSize-=2;
                StackSize = Math.max(StackSize, 0);
                StackSize++;
                MaxStackSize = Math.max(StackSize, MaxStackSize);
            }
            else {
                Operand variab = (Operand)value;
                String register = selectRegister(method, variab.getName());
                if(variab.getType().toString().equals("INT32") || variab.getType().toString().equals("BOOLEAN")){
                    aux += "\tiload" + register + '\n';
                    StackSize++;
                    MaxStackSize = Math.max(StackSize, MaxStackSize);
                }
                else{
                    aux += "\taload" + register + '\n';
                    StackSize++;
                    MaxStackSize = Math.max(StackSize, MaxStackSize);
                }
            }
        }
        else if(instr.getRhs() instanceof BinaryOpInstruction) {
            BinaryOpInstruction rinstr = (BinaryOpInstruction) instr.getRhs();
            Element lop = rinstr.getLeftOperand();
            Element rop = rinstr.getRightOperand();

            if ((!lop.isLiteral() && selectRegister(method, ((Operand) lop).getName()).equals(destreg) && rop.isLiteral() && (Integer.parseInt(((LiteralElement) rop).getLiteral()) <= 32767 && Integer.parseInt(((LiteralElement) rop).getLiteral()) >= -32768) && ((rinstr.getUnaryOperation().getOpType() == OperationType.ADD) || (rinstr.getUnaryOperation().getOpType() == OperationType.SUB)))
                    ^ (!rop.isLiteral() && (selectRegister(method, ((Operand) rop).getName()).equals(destreg)) && lop.isLiteral() && (Integer.parseInt(((LiteralElement) lop).getLiteral()) <= 32767 && Integer.parseInt(((LiteralElement) lop).getLiteral()) >= -32768) && (rinstr.getUnaryOperation().getOpType() == OperationType.ADD))) {
                String literal = lop.isLiteral() ? ((LiteralElement) lop).getLiteral() : ((LiteralElement) rop).getLiteral();
                if (rinstr.getUnaryOperation().getOpType() == OperationType.SUB) literal = "-" + literal;
                aux +=  "\tiinc" + destreg.replaceAll("_", " ") + " " + literal + "\n";
                opflag = true;
            } else {
                boolean zeroonright = false;
                if((lop.isLiteral() && ((LiteralElement) lop).getLiteral().equals("0") && rinstr.getUnaryOperation().getOpType() == OperationType.LTH) ||
                        (zeroonright = (rop.isLiteral() && ((LiteralElement) rop).getLiteral().equals("0") && rinstr.getUnaryOperation().getOpType() == OperationType.LTH))){
                    if(zeroonright){
                        if (lop.isLiteral()) {
                            LiteralElement llop = (LiteralElement) lop;
                            aux += "\t" + selectConstType(llop.getLiteral()) + '\n';
                        } else {
                            Operand olop = (Operand) lop;
                            String register = selectRegister(method, olop.getName());
                            if (olop.getType().toString().equals("INT32") || olop.getType().toString().equals("BOOLEAN")) {
                                aux += "\tiload" + register + '\n';
                                StackSize++;
                                MaxStackSize = Math.max(StackSize, MaxStackSize);
                            }
                        }
                        aux += "\tifge " + this.CurrentLabel + "\n";
                    }
                    else{
                        if (rop.isLiteral()) {
                            LiteralElement lrop = (LiteralElement) rop;
                            aux += "\t" + selectConstType(lrop.getLiteral()) + '\n';
                        } else {
                            Operand orop = (Operand) rop;
                            String register = selectRegister(method, orop.getName());
                            if (orop.getType().toString().equals("INT32") || orop.getType().toString().equals("BOOLEAN")) {
                                aux += "\tiload" + register + '\n';
                                StackSize++;
                                MaxStackSize = Math.max(StackSize, MaxStackSize);
                            }
                        }
                        aux += "\tifle " + this.CurrentLabel + "\n";
                    }
                    StackSize -= 1;
                    aux += "\ticonst_1\n";
                    StackSize++;
                    MaxStackSize = Math.max(StackSize, MaxStackSize);
                    aux += "\tgoto " + (this.CurrentLabel + 1) + "\n";
                    aux += "\t" + this.CurrentLabel + ": iconst_0\n";
                    StackSize++;
                    MaxStackSize = Math.max(StackSize, MaxStackSize);
                    this.CurrentLabel++;
                    auxlabel = this.CurrentLabel + ": ";
                    this.CurrentLabel++;
                }
                else {
                    if (lop.isLiteral()) {
                        LiteralElement llop = (LiteralElement) lop;
                        aux += "\t" + selectConstType(llop.getLiteral()) + '\n';
                    } else {
                        Operand olop = (Operand) lop;
                        String register = selectRegister(method, olop.getName());
                        if (olop.getType().toString().equals("INT32") || olop.getType().toString().equals("BOOLEAN")) {
                            aux += "\tiload" + register + '\n';
                            StackSize++;
                            MaxStackSize = Math.max(StackSize, MaxStackSize);
                        } else {
                            aux += "\taload" + register + '\n';
                            StackSize++;
                            MaxStackSize = Math.max(StackSize, MaxStackSize);
                        }
                    }
                    if (rop.isLiteral()) {
                        LiteralElement lrop = (LiteralElement) rop;
                        aux += "\t" + selectConstType(lrop.getLiteral()) + '\n';
                    } else {
                        Operand orop = (Operand) rop;
                        String register = selectRegister(method, orop.getName());
                        if (orop.getType().toString().equals("INT32") || orop.getType().toString().equals("BOOLEAN")) {
                            aux += "\tiload" + register + '\n';
                            StackSize++;
                            MaxStackSize = Math.max(StackSize, MaxStackSize);
                        }
                        else {
                            aux += "\taload" + register + '\n';
                            StackSize++;
                            MaxStackSize = Math.max(StackSize, MaxStackSize);
                        }
                    }
                    switch (rinstr.getUnaryOperation().getOpType()) {
                        case ADD:
                            aux += "\tiadd\n";
                            StackSize -= 2;
                            break;
                        case SUB:
                            aux += "\tisub\n";
                            StackSize -= 2;
                            break;
                        case MUL:
                            aux += "\timul\n";
                            StackSize -= 2;
                            break;
                        case DIV:
                            aux += "\tidiv\n";
                            StackSize -= 2;
                            break;
                        case AND:
                        case ANDB:
                            aux += "\tiand\n";
                            StackSize -= 2;
                            break;
                        case LTH:
                            aux += "\tif_icmpge " + this.CurrentLabel + "\n";
                            StackSize -= 2;
                            aux += "\ticonst_1\n";
                            StackSize++;
                            MaxStackSize = Math.max(StackSize, MaxStackSize);
                            aux += "\tgoto " + (this.CurrentLabel + 1) + "\n";
                            aux += "\t" + this.CurrentLabel + ": iconst_0\n";
                            StackSize++;
                            MaxStackSize = Math.max(StackSize, MaxStackSize);
                            this.CurrentLabel++;
                            auxlabel = this.CurrentLabel + ": ";
                            this.CurrentLabel++;
                            break;
                    }
                }
            }
        }
        else if(instr.getRhs() instanceof UnaryOpInstruction){
            UnaryOpInstruction rinstr = (UnaryOpInstruction)instr.getRhs();
            if(rinstr.getUnaryOperation().getOpType() == OperationType.NOT || rinstr.getUnaryOperation().getOpType() == OperationType.NOTB){
                aux+="\ticonst_1\n";
                StackSize++;
                MaxStackSize = Math.max(StackSize, MaxStackSize);
                Element var = rinstr.getRightOperand();
                if(var.isLiteral()){
                    aux+= "\t" + selectConstType(((LiteralElement)var).getLiteral()) + "\n";
                }
                else{
                    Operand varname = (Operand) var;
                    String register = selectRegister(method, varname.getName());
                    if(varname.getType().toString().equals("INT32") || varname.getType().toString().equals("BOOLEAN")){
                        aux += "\tiload" + register + '\n';
                        StackSize++;
                        MaxStackSize = Math.max(StackSize, MaxStackSize);
                    }
                    else{
                        aux += "\taload" + register + '\n';
                        StackSize++;
                        MaxStackSize = Math.max(StackSize, MaxStackSize);
                    }
                }
                aux += "\tisub\n";
                StackSize -= 2;
            }
        }
        else if(instr.getRhs() instanceof CallInstruction){
            aux += parseCallInstr(method, (CallInstruction)instr.getRhs(), false);
        }
        else if(instr.getRhs() instanceof GetFieldInstruction){
            GetFieldInstruction getfieldinstr = (GetFieldInstruction) instr.getRhs();
            aux+=parseGetFieldInstr(method, getfieldinstr);
        }
        if(!(instr.getDest() instanceof ArrayOperand) && !opflag){
            aux+= "\t" + auxlabel + ((instr.getDest().getType().getTypeOfElement() == ElementType.INT32 || instr.getDest().getType().getTypeOfElement() == ElementType.BOOLEAN) ? "i" : "a") +"store" + destreg + '\n';
            StackSize--;
            StackSize = Math.max(StackSize, 0);
        }
        return aux;
    }

    private String parseGetFieldInstr(Method method, GetFieldInstruction getfieldinstr){
        String result = "";
        String valtype2 = "V";
        valtype2 = generateType(getfieldinstr.getSecondOperand().getType());
        result += "\taload" + selectRegister(method ,((Operand)getfieldinstr.getFirstOperand()).getName()) + "\n";
        StackSize++;
        MaxStackSize = Math.max(StackSize, MaxStackSize);
        //result += "\tgetfield " + valtype2 + " " + ((Operand)getfieldinstr.getSecondOperand()).getName()+ "\n";
        result += "\tgetfield " + getFullImportName(((ClassType)getfieldinstr.getFirstOperand().getType()).getName()) + "/" + ((Operand)getfieldinstr.getSecondOperand()).getName() + " " + valtype2 + "\n";
        return result;
    }

    private String parseCallInstr(Method method, CallInstruction instr, boolean flag){
        String aux = "";
        boolean flag2 = false;
        switch (instr.getInvocationType()){
            case invokevirtual:
                aux += "\taload" + selectRegister(method, ((Operand)instr.getFirstArg()).getName()) + "\n";
                StackSize++;
                MaxStackSize = Math.max(StackSize, MaxStackSize);
                String funcstr = "\tinvokevirtual " + getFullImportName(((ClassType)instr.getFirstArg().getType()).getName()) + "." + ((LiteralElement)instr.getSecondArg()).getLiteral().replaceAll("\"", "") + "(";
                for(Element arg : instr.getListOfOperands()){
                    if(arg.isLiteral()){
                        aux+="\t" + selectConstType(((LiteralElement)arg).getLiteral()) +'\n';
                    }
                    else{
                        Operand argop = (Operand) arg;
                        if (argop.getType().toString().equals("INT32") || argop.getType().toString().equals("BOOLEAN")){
                            aux+="\tiload";
                            StackSize++;
                            MaxStackSize = Math.max(StackSize, MaxStackSize);
                        }
                        else{
                            aux+="\taload";
                            StackSize++;
                            MaxStackSize = Math.max(StackSize, MaxStackSize);
                        }
                        aux += selectRegister(method, argop.getName()) + "\n";
                    }
                    funcstr += generateType(arg.getType());
                }
                StackSize-= (instr.getListOfOperands().size()+1);
                StackSize = Math.max(StackSize, 0);
                funcstr+=")";
                funcstr += generateType(instr.getReturnType());
                if(instr.getReturnType().getTypeOfElement() != ElementType.VOID){
                    flag2 = true;
                    StackSize++;
                    MaxStackSize = Math.max(StackSize, MaxStackSize);
                }
                aux+= funcstr + "\n";
                break;
            case invokespecial:
                if(instr.getFirstArg().getType().toString().equals("THIS")){
                    aux+="\taload_0\n";
                    StackSize++;
                    MaxStackSize = Math.max(StackSize, MaxStackSize);
                    aux+="\tinvokespecial " + getFullImportName(((ClassType)instr.getFirstArg().getType()).getName()) + "/<init>()";
                    StackSize--;
                    StackSize = Math.max(StackSize, 0);
                    aux += generateType(instr.getReturnType());
                    aux+="\n";
                }
                break;
            case invokestatic:
                String funcstr2 = "\tinvokestatic " + getFullImportName(((Operand)instr.getFirstArg()).getName()) + "." + ((LiteralElement)instr.getSecondArg()).getLiteral().replaceAll("\"", "") + "(";
                for(Element arg : instr.getListOfOperands()){
                    if(arg.isLiteral()){
                        aux+="\t" + selectConstType(((LiteralElement)arg).getLiteral()) +'\n';
                    }
                    else{
                        Operand argop = (Operand) arg;
                        if (argop.getType().toString().equals("INT32") || argop.getType().toString().equals("BOOLEAN")){
                            aux+="\tiload";
                            StackSize++;
                            MaxStackSize = Math.max(StackSize, MaxStackSize);
                        }
                        else{
                            aux+="\taload";
                            StackSize++;
                            MaxStackSize = Math.max(StackSize, MaxStackSize);
                        }
                        aux += selectRegister(method, argop.getName()) + "\n";
                    }
                    funcstr2 += generateType(arg.getType());
                }
                StackSize-= instr.getListOfOperands().size();
                StackSize = Math.max(StackSize, 0);
                funcstr2+=")";
                funcstr2 += generateType(instr.getReturnType());
                if(instr.getReturnType().getTypeOfElement() != ElementType.VOID){
                    flag2 = true;
                    StackSize++;
                    MaxStackSize = Math.max(StackSize, MaxStackSize);
                }
                aux+= funcstr2 + "\n";
                break;
            case NEW:
                if(instr.getNumOperands() == 1){
                    aux+="\tnew " + getFullImportName(((Operand)instr.getFirstArg()).getName()) + "\n\tdup\n\tinvokespecial " + getFullImportName(((ClassType)instr.getFirstArg().getType()).getName()) + "/<init>()V\n";
                    StackSize+=2;
                    MaxStackSize = Math.max(StackSize, MaxStackSize);
                    StackSize--;
                    StackSize = Math.max(StackSize, 0);
                }
                else if(instr.getNumOperands() == 2){
                    Element elem = instr.getSecondArg() == null ? instr.getListOfOperands().get(0) : instr.getSecondArg();
                    if(elem.isLiteral()){
                        aux+="\t" + selectConstType(((LiteralElement)elem).getLiteral()) + "\n";
                    }
                    else{
                        aux+="\tiload" + selectRegister(method, ((Operand)elem).getName()) + "\n";
                        StackSize++;
                        MaxStackSize = Math.max(StackSize, MaxStackSize);
                    }
                    aux+="\tnewarray int\n";
                }
                break;
            case arraylength:
                String register = selectRegister(method, ((Operand)instr.getFirstArg()).getName());
                aux += "\taload" + register + '\n';
                StackSize++;
                MaxStackSize = Math.max(StackSize, MaxStackSize);
                aux+="\tarraylength\n";
                break;
        }
        if(flag && flag2){
            aux+="\tpop\n";
        }
        return aux;
    }
    private String selectRegister(Method method, String name){
        if (method.getVarTable().get(name) == null)
            System.out.println("carlos gay");
        int register = method.getVarTable().get(name).getVirtualReg();
        return register > 3 ? " " + register : "_" + register;
    }

    private String getFullImportName(String classname){
        String result = classname;
        for(String imp : this.imports){
            if(imp.contains(classname)){
                result = imp.replaceAll("\\.", "/");
                break;
            }
        }
        return result;
    }

    private String generateType(Type type) {
        String result = "V";
        switch (type.getTypeOfElement().name()) {
            case "INT32":
                result = "I";
                break;
            case "BOOLEAN":
                result = "Z";
                break;
            case "ARRAYREF":
                var arrayType = ((ArrayType) type);
                if (arrayType.getTypeOfElements().name().equals("INT32")) {
                    result = "[I";
                }

                else if (arrayType.getTypeOfElements().name().equals("STRING")) {
                    result = "[Ljava/lang/String;";
                }
                break;
            case "OBJECTREF":
                result = "L" + getFullImportName(((ClassType) type).getName());
                break;
            case "VOID":
                result = "V";
                break;
        }
        return result;
    }

    private String selectConstType(String literal){
        StackSize++;
        MaxStackSize = Math.max(StackSize, MaxStackSize);
        return Integer.parseInt(literal) < -1 || Integer.parseInt(literal) > 5 ? Integer.parseInt(literal) < -128 || Integer.parseInt(literal) > 127 ? Integer.parseInt(literal) < -32768 || Integer.parseInt(literal) > 32767 ? "ldc " + literal : "sipush " + literal : "bipush " + literal : "iconst_" + literal;
    }
}
