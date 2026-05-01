package dev.muon.dynamic_resource_bars.util;

public record Position(int x, int y) {
    public Position offset(int x, int y) {
        return new Position(this.x + x, this.y + y);
    }
}