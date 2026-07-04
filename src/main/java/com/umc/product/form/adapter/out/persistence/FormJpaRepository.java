package com.umc.product.form.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import com.umc.product.form.domain.Form;

public interface FormJpaRepository extends JpaRepository<Form, Long> {
}
