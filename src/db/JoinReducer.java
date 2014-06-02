package db;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.mapreduce.Reducer;

import db.sql.BoolExpr;
import db.sql.BoolExprFactory;
import db.sql.Evaluator;
import db.sql.EvaluatorFactory;
import db.sql.RowEvaluationClosure;
import db.sql.RowEvaluatorFactory;
import db.sql.WhereParser;
import db.table.IntField;
import db.table.JoinRowFactory;
import db.table.JoinedRow;
import db.table.Row;
import db.table.Schema;
import db.table.SchemaUtils;

public class JoinReducer extends Reducer<BytesWritable, BytesWritable, BytesWritable, BytesWritable> {
	
	public static final Log LOG = LogFactory.getLog(JoinReducer.class);
	JoinedRow joinedRow;
	JoinRowFactory factory;
	Schema leftValueSchema;
	Schema rightValueSchema;
	BytesWritable tKey;
	BytesWritable tValue;
	RowEvaluationClosure rowClosure = null;
	Evaluator evaluator = null;
	
	public void setup(Context context) {
		Configuration conf = context.getConfiguration();
		String join_using_columns = conf.get(Constant.JOIN_USING);
		
		Schema result_schema = Schema.createSchema(conf.get(Constant.JOIN_RESULT_SCHEMA));
		Schema leftSchema = Schema.createSchema(conf.get(Constant.LEFT_JOIN_SCHEMA));
		Schema rightSchema = Schema.createSchema(conf.get(Constant.RIGHT_JOIN_SCHEMA));
		
		int[] lKeyColumnIndexes = Schema.columnIndexes(leftSchema,
				SchemaUtils.parseColumns(join_using_columns));
		int[] lValueColumnIndexes = SchemaUtils.columnLeft(lKeyColumnIndexes, leftSchema
				.getRecordDescriptor().size());
		leftValueSchema = leftSchema.createSubSchema(lValueColumnIndexes);
		
		int[] rKeyColumnIndexes = Schema.columnIndexes(rightSchema,
				SchemaUtils.parseColumns(join_using_columns));
		int[] rValueColumnIndexes = SchemaUtils.columnLeft(rKeyColumnIndexes, rightSchema
				.getRecordDescriptor().size());
		
		rightValueSchema = rightSchema.createSubSchema(rValueColumnIndexes);
		
		String whereStr = conf.get(Constant.WHERE);
		if(whereStr != null) {
			BoolExpr whereExpr = new WhereParser(whereStr).parseBoolExpr();
			RowEvaluatorFactory rowEvalFactory = new RowEvaluatorFactory();
			rowClosure = rowEvalFactory.getCloseure();
			rowClosure.setSchema(result_schema);
			evaluator = whereExpr.createEvaluator(rowEvalFactory);
		}
		
		joinedRow = JoinedRow.createBySchema(result_schema, leftSchema, rightSchema, join_using_columns);
		factory = new JoinRowFactory();
		tKey = Constant.EMPTY_BYTESWRITABLE;
		tValue = new BytesWritable();
	}
	
	protected void reduce(BytesWritable key, Iterable<BytesWritable> values,
			Context context) throws IOException, InterruptedException {
		joinedRow.initByEquiColumns(key);
		
		List<Row> leftRows = new ArrayList<Row>();
		List<Row> rightRows = new ArrayList<Row>();
		List<Row> currentList;
		
		for(BytesWritable bytes: values) {
			IntField markField = factory.readOneFieldFromBytes(bytes);
			Row row;
			if(markField.equals(Row.fieldMarkLeft)) {
				currentList = leftRows;
				row = Row.createBySchema(leftValueSchema);
				LOG.fatal("LEFT : " + joinedRow.getFields()[0].toString());
			} else {
				currentList = rightRows;
				row = Row.createBySchema(rightValueSchema);
				LOG.fatal("RIGHT: " + joinedRow.getFields()[0].toString());
			}
			factory.readRemaining(row);
			LOG.fatal(row.getFields()[0].toString());
			currentList.add(row);
		}
		
		for(Row left : leftRows) {
			joinedRow.setCursorOnLeft();
			joinedRow.push(left);
			for(Row right:rightRows) {
				joinedRow.setCursorOnRight();
				joinedRow.push(right);
				joinedRow.writeToBytes(tValue);
				if(evaluator != null) {
					rowClosure.setRow(joinedRow);
					if(evaluator.evalutate()) {
						context.write(tKey, tValue);
					}
				}
			}
		}
	}

}
