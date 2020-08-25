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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.task.configuration.EnableTask;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.RowMapper;

@EnableBatchProcessing
@Configuration
@EnableTask
public class PurchaseOrderReportConfiguration {

	@Autowired
	private JobBuilderFactory jobBuilderFactory;

	@Autowired
	private StepBuilderFactory stepBuilderFactory;

	private static final Log logger = LogFactory.getLog(PurchaseOrderReportConfiguration.class);

	@Bean
	public Job processPurchaseOrders(Step step) {
		return jobBuilderFactory.get("processPurchaseOrders").
				incrementer(new RunIdIncrementer()).
				listener(new JobExecutionResultListener()).
				flow(step).
				end().
				build();
	}

	@Bean
	public Step step1(ItemReader itemReader, ItemWriter itemWriter) {
		return stepBuilderFactory.get("step1").<Map<Object, Object>, Map<Object, Object>>chunk(10).
				reader(itemReader).
				writer(itemWriter).
				build();
	}

	@Bean
	public ItemReader<Map<Object, Object>> itemReader(DataSource dataSource, RowMapper rowMapper) {
		return new JdbcCursorItemReaderBuilder<Map<Object, Object>>().sql("SELECT user_id, sku, quantity, amount, mode FROM purchase_orders").
				name("purchase_order_reader").rowMapper(rowMapper)
				.dataSource(dataSource).build();
	}

	@Bean
	public RowMapper<Map<Object, Object>> rowMapper() {
		return new MapRowMapper();
	}

	@Bean
	public ItemWriter<Map<Object, Object>> itemWriter() {
		return new LogItemWriter();
	}

	private static class JobExecutionResultListener implements JobExecutionListener {
		@Override
		public void beforeJob(JobExecution jobExecution) {

		}

		@Override
		public void afterJob(JobExecution jobExecution) {
			Collection<StepExecution> stepExecutionCollection = jobExecution.getStepExecutions();
			Object[] stepExecutions = stepExecutionCollection.toArray();
			StepExecution stepExecution = (StepExecution) stepExecutions[0];
			logger.info("Total =\t" + stepExecution.getExecutionContext().getDouble("currentTotal"));
		}
	}
	public static class MapRowMapper implements RowMapper<Map<Object, Object>> {

		@Override
		public Map<Object, Object> mapRow(ResultSet rs, int rowNum) throws SQLException {
			Map<Object, Object> item = new HashMap<>(rs.getMetaData().getColumnCount());

			for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
				item.put(rs.getMetaData().getColumnName(i), rs.getObject(i));
			}

			return item;
		}

	}

}
