package net.minecraftforge.fml.common.gameevent;

public class TickEvent {
    public Phase phase;
    public enum Phase { START, END }
    public static class ClientTickEvent extends TickEvent {}
    public static class RenderTickEvent extends TickEvent {}
}
