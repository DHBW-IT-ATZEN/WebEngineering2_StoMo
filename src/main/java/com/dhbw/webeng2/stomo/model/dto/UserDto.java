package com.dhbw.webeng2.stomo.model.dto;

import com.dhbw.webeng2.stomo.model.entity.PortfolioItem;
import com.dhbw.webeng2.stomo.model.enums.Status;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class UserDto {
    private Long id;
    private String firstname;
    private String lastname;
    private String email;
    private String password;
    private Status status;
    private List<PortfolioItem> portfolioItems;
}
