package xyz.erupt.annotation.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * @author YuePeng
 * date 2021/11/11 22:41
 */
@Getter
@Setter
@Builder
public class Column {

    private String value;

    private int colspan = 1;

    private String style;

    public Column(String value) {
        this.value = value;
    }

    public Column(String value, int colspan) {
        this.value = value;
        this.colspan = colspan;
    }

    public Column(String value, int colspan, String style) {
        this.value = value;
        this.colspan = colspan;
        this.style = style;
    }

    public Column() {
    }
}
