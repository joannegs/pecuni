package com.pecuni.workspace;

import com.pecuni.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "workspace")
@Getter
@Setter
@NoArgsConstructor
public class Workspace extends BaseEntity {

    @Column(nullable = false, length = 150)
    private String name;
}
