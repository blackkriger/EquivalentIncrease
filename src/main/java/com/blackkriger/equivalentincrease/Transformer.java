package com.blackkriger.equivalentincrease;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

public class Transformer implements IClassTransformer {

    private static final String ITEM_EMC_BOOK_TARGET =
            "com.mordenkainen.equivalentenergistics.items.ItemEMCBook";
    private static final String TILE_EMC_CONDENSER_BASE_TARGET =
            "com.mordenkainen.equivalentenergistics.blocks.condenser.tiles.TileEMCCondenserBase";
    private static final String EE_PROJECTE_INTEGRATION_TARGET =
            "com.mordenkainen.equivalentenergistics.integration.projecte.ProjectE";

    private static final String EMC_ROUTER_INTERNAL = "com/blackkriger/equivalentincrease/EmcRouter";
    private static final String IEMC_STORAGE_GRID_INTERNAL =
            "com/mordenkainen/equivalentenergistics/integration/ae2/cache/storage/IEMCStorageGrid";
    private static final String ADDEMC_DESC = "(DLappeng/api/config/Actionable;)D";
    private static final String ROUTER_ROUTE_DESC =
            "(L" + IEMC_STORAGE_GRID_INTERNAL + ";DLappeng/api/config/Actionable;)D";
    private static final String GETAVAIL_DESC = "()D";
    private static final String ROUTER_AVAIL_DESC =
            "(L" + IEMC_STORAGE_GRID_INTERNAL + ";)D";

    private static final String IEMC_PROXY_INTERNAL = "moze_intel/projecte/api/proxy/IEMCProxy";

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null) return null;
        if (ITEM_EMC_BOOK_TARGET.equals(transformedName)) {
            return patchItemEMCBookRightClick(basicClass);
        }
        if (TILE_EMC_CONDENSER_BASE_TARGET.equals(transformedName)) {
            return patchCondenserAddEMC(basicClass);
        }
        if (EE_PROJECTE_INTEGRATION_TARGET.equals(transformedName)) {
            return patchEEProjectEGetValueLong(basicClass);
        }
        return basicClass;
    }

    private byte[] patchItemEMCBookRightClick(byte[] classBytes) {
        try {
            ClassReader cr = new ClassReader(classBytes);
            ClassNode cn = new ClassNode();
            cr.accept(cn, 0);

            int patched = 0;
            for (Object o : cn.methods) {
                MethodNode mn = (MethodNode) o;
                if (!isRightClickMethod(mn)) continue;

                LabelNode skipReturn = new LabelNode();
                InsnList prologue = new InsnList();
                prologue.add(new VarInsnNode(Opcodes.ALOAD, 1));
                prologue.add(new VarInsnNode(Opcodes.ALOAD, 2));
                prologue.add(new VarInsnNode(Opcodes.ALOAD, 3));
                prologue.add(new MethodInsnNode(
                        Opcodes.INVOKESTATIC,
                        EMC_ROUTER_INTERNAL,
                        "tryToggleRedirect",
                        "(Lnet/minecraft/item/ItemStack;Lnet/minecraft/world/World;Lnet/minecraft/entity/player/EntityPlayer;)Z",
                        false));
                prologue.add(new JumpInsnNode(Opcodes.IFEQ, skipReturn));
                prologue.add(new VarInsnNode(Opcodes.ALOAD, 1));
                prologue.add(new InsnNode(Opcodes.ARETURN));
                prologue.add(skipReturn);

                mn.instructions.insert(prologue);
                patched++;
            }

            if (patched == 0) {
                System.err.println("[EquivalentIncrease] WARN: ItemEMCBook.onItemRightClick not found — toggle disabled");
                return classBytes;
            }
            System.out.println("[EquivalentIncrease] ItemEMCBook: right-click toggle injected");

            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
            cn.accept(cw);
            return cw.toByteArray();
        } catch (Throwable t) {
            System.err.println("[EquivalentIncrease] ItemEMCBook patch failed: " + t);
            t.printStackTrace();
            return classBytes;
        }
    }

    private static boolean isRightClickMethod(MethodNode mn) {
        if (!"(Lnet/minecraft/item/ItemStack;Lnet/minecraft/world/World;Lnet/minecraft/entity/player/EntityPlayer;)Lnet/minecraft/item/ItemStack;"
                .equals(mn.desc)) return false;
        return "func_77659_a".equals(mn.name) || "onItemRightClick".equals(mn.name);
    }

    private byte[] patchCondenserAddEMC(byte[] classBytes) {
        try {
            ClassReader cr = new ClassReader(classBytes);
            ClassNode cn = new ClassNode();
            cr.accept(cn, 0);

            int redirected = 0;
            int availPatched = 0;
            for (Object o : cn.methods) {
                MethodNode mn = (MethodNode) o;
                AbstractInsnNode insn = mn.instructions.getFirst();
                while (insn != null) {
                    AbstractInsnNode next = insn.getNext();
                    if (insn.getOpcode() == Opcodes.INVOKEINTERFACE && insn instanceof MethodInsnNode) {
                        MethodInsnNode call = (MethodInsnNode) insn;
                        if (IEMC_STORAGE_GRID_INTERNAL.equals(call.owner)
                                && "addEMC".equals(call.name)
                                && ADDEMC_DESC.equals(call.desc)) {
                            MethodInsnNode replacement = new MethodInsnNode(
                                    Opcodes.INVOKESTATIC,
                                    EMC_ROUTER_INTERNAL,
                                    "routeOrStore",
                                    ROUTER_ROUTE_DESC,
                                    false);
                            mn.instructions.set(call, replacement);
                            redirected++;
                        } else if ("getAvail".equals(call.name)
                                && GETAVAIL_DESC.equals(call.desc)) {
                            MethodInsnNode replacement = new MethodInsnNode(
                                    Opcodes.INVOKESTATIC,
                                    EMC_ROUTER_INTERNAL,
                                    "availOrRoute",
                                    ROUTER_AVAIL_DESC,
                                    false);
                            mn.instructions.set(call, replacement);
                            availPatched++;
                        }
                    }
                    insn = next;
                }
            }

            if (redirected == 0) {
                System.err.println("[EquivalentIncrease] WARN: TileEMCCondenserBase has no IEMCStorageGrid.addEMC calls — condenser routing disabled");
                return classBytes;
            }
            System.out.println("[EquivalentIncrease] TileEMCCondenserBase: " + redirected + " addEMC call(s) redirected, "
                    + availPatched + " getAvail call(s) redirected");

            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
            cn.accept(cw);
            return cw.toByteArray();
        } catch (Throwable t) {
            System.err.println("[EquivalentIncrease] TileEMCCondenserBase patch failed: " + t);
            t.printStackTrace();
            return classBytes;
        }
    }

    /**
     * Bridge for the FMProjectE 2.x API change: IEMCProxy.getValue(...) now returns
     * long, EE 0.8.3 was compiled against the old int variant. Every call site in
     * EE's ProjectE integration that expected (...)I gets its desc rewritten to
     * (...)J followed by an L2I conversion so the surrounding bytecode (which still
     * treats the result as int) stays balanced. Also patches getValue(Block) and
     * getValue(Item) overloads for the same reason.
     */
    private byte[] patchEEProjectEGetValueLong(byte[] classBytes) {
        try {
            ClassReader cr = new ClassReader(classBytes);
            ClassNode cn = new ClassNode();
            cr.accept(cn, 0);

            int rewritten = 0;
            for (Object o : cn.methods) {
                MethodNode mn = (MethodNode) o;
                AbstractInsnNode insn = mn.instructions.getFirst();
                while (insn != null) {
                    AbstractInsnNode next = insn.getNext();
                    if (insn.getOpcode() == Opcodes.INVOKEINTERFACE && insn instanceof MethodInsnNode) {
                        MethodInsnNode call = (MethodInsnNode) insn;
                        if (IEMC_PROXY_INTERNAL.equals(call.owner)
                                && "getValue".equals(call.name)
                                && call.desc != null && call.desc.endsWith(")I")) {
                            call.desc = call.desc.substring(0, call.desc.length() - 2) + ")J";
                            mn.instructions.insert(call, new InsnNode(Opcodes.L2I));
                            rewritten++;
                        }
                    }
                    insn = next;
                }
            }

            if (rewritten == 0) {
                System.err.println("[EquivalentIncrease] WARN: EE ProjectE integration — no IEMCProxy.getValue(...)I calls found, FMProjectE compat skip");
                return classBytes;
            }
            System.out.println("[EquivalentIncrease] EE ProjectE integration: " + rewritten + " IEMCProxy.getValue(...)I → (...)J + L2I bridge installed");

            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
            cn.accept(cw);
            return cw.toByteArray();
        } catch (Throwable t) {
            System.err.println("[EquivalentIncrease] EE ProjectE patch failed: " + t);
            t.printStackTrace();
            return classBytes;
        }
    }
}
