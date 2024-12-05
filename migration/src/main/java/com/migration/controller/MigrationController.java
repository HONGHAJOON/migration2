package com.migration.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import com.migration.service.MigrationService;

@Controller
public class MigrationController {
	@Autowired
	private MigrationService migrationService;
	
	
}
