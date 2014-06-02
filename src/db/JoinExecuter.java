package db;

import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.MultipleInputs;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.Tool;

import db.table.Schema;
import db.table.SchemaUtils;

public class JoinExecuter extends Configured implements Tool {

	
	private List<String> usingColumns;
	private Schema leftSchema;
	private Schema rightSchema;
	private Schema outputSchema;

	@Override
	public int run(String[] args) throws Exception {
		Configuration conf = getConf();

		//TODO: set where filter
		
		conf.set(Constant.LEFT_JOIN_SCHEMA, leftSchema.toString());
		conf.set(Constant.RIGHT_JOIN_SCHEMA, rightSchema.toString());
		conf.set(Constant.JOIN_RESULT_SCHEMA, outputSchema.toString() );
		conf.set(Constant.JOIN_USING,  SchemaUtils.dumpColumns(usingColumns));

		
		Job job = new Job(conf, String.format("%s = %s * %s",
				outputSchema.getTableName(), leftSchema.getTableName(),
				rightSchema.getTableName()));
		job.setJarByClass(getClass());
		
		
		MultipleInputs.addInputPath(job, DbPathUtils.tablePath(leftSchema),
				SequenceFileInputFormat.class, LeftJoinMapper.class);

		MultipleInputs.addInputPath(job, DbPathUtils.tablePath(rightSchema),
				SequenceFileInputFormat.class, RightJoinMapper.class);
		
		job.setReducerClass(JoinReducer.class);
	    FileOutputFormat.setOutputPath(job, DbPathUtils.tablePath(outputSchema));
		job.setOutputKeyClass(BytesWritable.class);
		job.setOutputKeyClass(BytesWritable.class);
		
		return job.waitForCompletion(true) ? 0 : 1;
	}
	
	
	public void setUsingColumns(List<String> usingColumns) {
		this.usingColumns = usingColumns;
	}

	public void setLeftSchema(Schema leftSchema) {
		this.leftSchema = leftSchema;
	}

	public void setRightSchema(Schema rightSchema) {
		this.rightSchema = rightSchema;
	}

	public void setOutputSchema(Schema outputSchema) {
		this.outputSchema = outputSchema;
	}

}
