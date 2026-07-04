package com.umc.product.form.adapter.out.persistence;

import com.umc.product.form.domain.Form;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FormJpaRepository extends JpaRepository<Form, Long> {
}
