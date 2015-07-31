<pre><code>
package com.monotrack.entities;


import com.google.common.base.Objects;
import org.hibernate.validator.constraints.NotEmpty;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

@Entity
@Table(name = "aircamp_news")
public class AircampNews {

    private Long id;
    private String title;
    private String message;
    private User creator;
    private Long creation_time;
    private Long update_time;

    public AircampNews(String title, String message, User creator, Long creation_time, Long update_time) {
        this.title = title;
        this.message = message;
        this.creator = creator;
        this.creation_time = creation_time;
        this.update_time = update_time;
    }

    public AircampNews() {
    }

    @Id
    @SequenceGenerator(name = "aircamp_news_seq", sequenceName = "aircamp_news_seq")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "aircamp_news_seq")
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @NotEmpty
    @Column(name = "title")
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @NotEmpty
    @Column(name = "message")
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id")
    public User getCreator() {
        return creator;
    }

    public void setCreator(User creator) {
        this.creator = creator;
    }

    @NotNull
    @Column(name = "creation_time")
    public Long getCreation_time() {
        return creation_time;
    }

    public void setCreation_time(Long creation_time) {
        this.creation_time = creation_time;
    }

    @Column(name = "update_time")
    public Long getUpdate_time() {
        return update_time;
    }

    public void setUpdate_time(Long update_time) {
        this.update_time = update_time;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("id", id)
                .add("title", title)
                .add("message", message)
                .add("creator", creator)
                .add("creation_time", creation_time)
                .add("update_time", update_time)
                .toString();
    }
}
</code></pre>1
