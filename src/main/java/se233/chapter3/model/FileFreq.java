package se233.chapter3.model;

public class FileFreq {
    public String getPath() {
        return path;
    }

    private String name;
    private String path;
    private Integer freq;
    public FileFreq(String name, String path, Integer freq) {
        this.name = name;
        this.path = path;
        this.freq = freq;
    }

    @Override
    public String toString() {
        return String.format("{%s:%d}",name,freq);
    }
}
