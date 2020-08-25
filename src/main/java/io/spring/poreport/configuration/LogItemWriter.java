/*
 * Copyright 2020 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.spring.poreport.configuration;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemWriter;

public class LogItemWriter implements ItemWriter<Map<Object, Object>>, ItemStream {
	private double currentTotal = 0;

	private static final Log logger = LogFactory.getLog(LogItemWriter.class);
	@Override
	public void write(List<? extends Map<Object, Object>> items) throws Exception {
		for(Map<Object, Object> item : items) {
			Integer quantity = (Integer)item.get("quantity");
			Double amount = ((BigDecimal)item.get("amount")).doubleValue();
			String mode = (String)item.get("mode");
			this.currentTotal += amount * quantity;
			logger.info(String.format("%s\t%s\t%s\t%s\t%s",item.get("user_id"),item.get("sku"),quantity, amount, mode));
		}
	}

	@Override
	public void open(ExecutionContext executionContext) throws ItemStreamException {
		logger.info("user_id\tsku\tquantity\tamount\tmode");
	}

	@Override
	public void update(ExecutionContext executionContext) throws ItemStreamException {
		executionContext.put("currentTotal", this.currentTotal);
	}

	@Override
	public void close() throws ItemStreamException {

	}
}
