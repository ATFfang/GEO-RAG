package com.EarthCube.georag_backend.common.result;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Data;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

@Data
public class PageResult<T> implements Serializable {

    /**
     * 当前页数据列表
     */
    private List<T> records;

    /**
     * 总条数
     */
    private Long total;

    /**
     * 当前页码
     */
    private Long current;

    /**
     * 每页大小
     */
    private Long size;

    /**
     * 总页数
     */
    private Long pages;

    public PageResult() {
    }

    /**
     * 将 MyBatis Plus 的 IPage 转换为通用的 PageResult
     */
    public PageResult(IPage<T> page) {
        this.records = page.getRecords();
        this.total = page.getTotal();
        this.current = page.getCurrent();
        this.size = page.getSize();
        this.pages = page.getPages();
    }

    /**
     * 静态工厂方法：从 MP 的 Page 对象构建
     */
    public static <T> PageResult<T> of(IPage<T> page) {
        return new PageResult<>(page);
    }

    /**
     * 静态工厂方法：构建空分页（用于异常或空数据场景）
     */
    public static <T> PageResult<T> empty(Long current, Long size) {
        PageResult<T> result = new PageResult<>();
        result.setRecords(Collections.emptyList());
        result.setTotal(0L);
        result.setCurrent(current);
        result.setSize(size);
        result.setPages(0L);
        return result;
    }
}