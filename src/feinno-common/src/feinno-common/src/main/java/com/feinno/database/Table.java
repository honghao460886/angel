package com.feinno.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * 
 * <b>描述: </b>此类为数据库"表结构-->JAVA对象的映射"，通过JAVA对象形式表现数据库的表结构<br>
 * 暂时支持<code>varchar、text、int、long、double、datetime、timestamp</code>类型，
 * ,以及设置主键、自增字段、默认值等功能，创建对象后调用<code>toString()</code>方法可将对象转换为SQL语句
 * 此类为了保证数据库表结构的约束性，不对外提供对象成员的修改方法。
 * <p>
 * <b>功能: </b>提供 数据库"表结构-->JAVA对象的映射"
 * <p>
 * <b>用法: </b>此类与{@link MonDBHelper}配合使用效果最佳
 * 
 * <pre>
 * String tableName = ...
 * Table table = new Table(tableName);
 * table.addColumns(Table.Column.createAutoIncrementIntColumn(&quot;UserId&quot;));
 * table.addColumns(Table.Column.createVarcharColumn(&quot;Name&quot;, 20, true, null));
 * table.addColumns(Table.Column.createIntColumn(&quot;Age&quot;, false, 20));
 * table.addColumns(Table.Column.createDateTimeColumn(&quot;Birthday&quot;, false, &quot;'2000-1-1'&quot;));
 * table.addPrimaryKey(&quot;UserId&quot;);
 * System.out.println(table.toString); // 输出该表的表建表语句
 * 
 * MonDBHelper monDBHelper = DatabaseManager.getMonDBHelper("test", dbConfigs);
 * monDBHelper.createTable(table); //创建表
 * Table table = monDBHelper.getTable(tableName) //获得表结构
 * </pre>
 * <p>
 * 
 * @author Lv.Mingwei
 * @see MonDBHelper
 */
public class Table {
	private String tableName;
	private List<Column> columns = null;
	private List<String> primaryKeys = null;
	private String extension = null;

	public Table(String tableName) {
		this.tableName = tableName;
	}

	public String getTableName() {
		return tableName;
	}

	/**
	 * 此处用于设置扩展的建表语句，该语句会包含在{@link Table#toString()} 方法生成的建表脚本中，<br>
	 * 可以用它加入例如索引等扩展语句
	 * 
	 * @param extendSQL
	 */
	public void setExtension(String extension) {
		this.extension = extension;
	}

	/**
	 * 为表添加列字段
	 * 
	 * @param column
	 * @return 当出现以下情况之一时返回<code>false</code><br>
	 *         1. 列字段格式有问题<br>
	 *         2. 当前表中已经存在该名称的字段
	 */
	public boolean addColumns(Column column) {

		// 列字段格式自检未通过，说明格式有问题，因此返回添加失败
		if (column == null || !column.check()) {
			return false;
		}

		if (columns == null) {
			columns = new ArrayList<Column>();
		}

		// 不允许添加列名相同的字段(不区分大小写)
		for (Column columnTemp : columns) {
			if (columnTemp.getName().equalsIgnoreCase(column.getName())) {
				return false;
			}
		}
		return columns.add(column);
	}

	/**
	 * 向{@link Table}中增加主键，增加主键时必须要保证该对象中以存在相应的列
	 * 
	 * @param primaryKey
	 * @return 当出现以下情况之一时返回<code>false</code><br>
	 *         1. 如果列字段中不存在以该主键命名的字段 <br>
	 *         2. 如果主键集合中已经存在该主键<br>
	 */
	public boolean addPrimaryKey(String primaryKey) {

		if (columns == null || columns.size() == 0) {
			return false;
		}

		for (Column column : columns) {
			if (column == null || column.getName() == null) {
				continue;
			}
			if (column.getName().equalsIgnoreCase(primaryKey)) {
				if (primaryKeys == null) {
					primaryKeys = new ArrayList<String>();
				}
				if (primaryKeys.contains(primaryKey)) {
					return false;
				}
				// 如果为主键,则当前列不能为空
				column.setNull(false);
				return primaryKeys.add(primaryKey);
			}
		}
		return false;
	}

	/**
	 * 覆盖的toString()方法,对象的表示方式既为SQL语句
	 */
	@Override
	public String toString() {

		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("create table `").append(tableName).append("`(");
		// Step1. 拼接列
		if (columns != null && columns.size() != 0) {
			for (Column column : columns) {
				stringBuilder.append(column.toString()).append(",");
				if (column.isAutoIncrement()) {
					// 如果某一列是自增字段,那么此列默认是主键中的一个
					this.addPrimaryKey(column.getName());
				}
			}
		}
		// Step2. 拼接主键
		if (primaryKeys != null && primaryKeys.size() != 0) {
			stringBuilder.append("primary key (");
			for (String primaryKey : primaryKeys) {
				stringBuilder.append("`").append(primaryKey).append("`,");
			}
			stringBuilder.delete(stringBuilder.length() - 1, stringBuilder.length()).append(")").append(",");
		}
		if (stringBuilder.charAt(stringBuilder.length() - 1) == ',') {
			stringBuilder.delete(stringBuilder.length() - 1, stringBuilder.length());
		}
		// Step3. 拼接扩展内容，此扩展内容可以包括例如索引等SQL
		if (extension != null && extension.length() > 0) {
			stringBuilder.append(",");
			stringBuilder.append(extension);
		}
		stringBuilder.append(")");
		return stringBuilder.toString();
	}

	/**
	 * 覆盖equals方法，目的为用于两个数据库表结构的比较
	 */
	@Override
	public boolean equals(Object obj) {

		if (obj == null) {
			return false;
		}

		// 如果指向同一内存地址，不用比对了，直接相同
		if (this == obj) {
			return true;
		}

		if (this.getClass() != obj.getClass()) {
			return false;
		}

		if (!(obj instanceof Table)) {
			return false;
		}

		Table table = (Table) obj;

		if (this.tableName != null) {
			if (!this.tableName.equalsIgnoreCase(table.tableName)) {
				return false;
			}
		} else if (table.tableName != null) {
			return false;
		}

		// 比对列
		if (this.columns != null && table.columns != null) {
			if (this.columns.size() != table.columns.size()) {
				return false;
			} else {
				for (Column column1 : columns) {
					if (column1.isAutoIncrement) {
						this.addPrimaryKey(column1.getName());
					}
					// 如果有一个不相同的，则返回不同
					if (!table.columns.contains(column1)) {
						return false;
					}
				}
			}
		} else if (this.columns == null && table.columns != null) {
			return false;
		} else if (this.columns != null && table.columns == null) {
			return false;
		}

		// 比对主键
		if (this.primaryKeys != null && table.primaryKeys != null) {
			if (this.primaryKeys.size() != table.primaryKeys.size()) {
				return false;
			} else {
				boolean isSame = false;
				for (String primaryKey1 : primaryKeys) {
					isSame = false;
					for (String primaryKey2 : table.primaryKeys) {
						if (primaryKey1.equals(primaryKey2)) {
							isSame = true;
							break;
						}
					}
					// 在数量相同的前提下，如果有一个不相同的，则返回不同
					if (!isSame) {
						return false;
					}
				}
			}
		} else if (this.primaryKeys == null && table.primaryKeys != null) {
			return false;
		} else if (this.primaryKeys != null && table.primaryKeys == null) {
			return false;
		}

		return true;
	}

	/**
	 * 因为{@link Object#equals()}方法被覆盖了，为了防止{@link Table}被放置在Map中找不到对象，此处也覆盖了
	 * {@link Object#hashCode()}方法
	 */
	@Override
	public int hashCode() {
		int result = 17;
		result = 31 * result + (tableName != null ? tableName.hashCode() : 0);
		if (columns != null) {
			for (Column column : columns)
				result = 31 * result + column.hashCode();
		}
		if (primaryKeys != null) {
			for (String primaryKey : primaryKeys)
				result = 31 * result + primaryKey.hashCode();
		}
		return result;
	}

	/**
	 * 将{@link ResultSet}类型转换为{@link Table}类型的表结构对象
	 * 
	 * @param rs
	 * @return
	 * @throws SQLException
	 */
	public static Table valueOf(ResultSet rs) throws SQLException {
		if (rs == null) {
			return null;
		}
		Table table = null;
		while (rs.next()) {
			if (table == null) {
				table = new Table(rs.getString("TABLE_NAME"));
			}
			table.addColumns(Column.valueOf(rs));
		}

		return table;
	}

	/**
	 * 为了保证{@link Table }
	 * 相对于数据库表的约束性是相同的，因此会隐藏部分数据，防止该对象的数据被篡改，但是这样会影响到类的创建者无法看到类中的数据，
	 * 也就无法检查此类是否适合创建数据库表 基于这个原因这里提供了一个方法， 用以在提交时检验此对象是否符合数据库表格要求<br>
	 * {@Table }对象是否符合数据库表结构的要求
	 * 
	 * @return
	 */
	public boolean check() {
		// 验证表格名称是否存在
		if (tableName == null || tableName.length() == 0) {
			return false;
		}
		// 验证列是否存在
		if (columns == null || columns.size() == 0) {
			return false;
		}
		// 验证列的格式是否正确
		for (Column column : columns) {
			if (column == null || !column.check()) {
				return false;
			}
		}
		// 如果存在主键，则主键名称一定要是一个列的名字
		if (primaryKeys != null) {
			for (String primaryKey : primaryKeys) {
				boolean isSuccess = false;
				for (Column column : columns) {
					if (column.getName().equalsIgnoreCase(primaryKey)) {
						isSuccess = true;
						break;
					}
				}
				if (!isSuccess) {
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * 数据库中的列格式类型
	 * 
	 * @author Lv.Mingwei
	 * 
	 */
	public static class Column {
		private String name;
		private ColumnType columnType;
		private Integer length;
		private boolean isNull;
		private boolean isAutoIncrement;
		private String defauleValue;
		private static final int INT_DEFAULT_LENGTH = 11;
		private static final int LONG_DEFAULT_LENGTH = 63;

		/**
		 * 为提高安全性,保证每一列的类型与格式相对应，符合数据库的规范要求，因此屏蔽了默认的构造方法
		 */
		private Column() {
		}

		/**
		 * 创建一个varchar类型的列
		 * 
		 * @param name
		 *            列名称
		 * @param length
		 *            列长度
		 * @param isNull
		 *            是否为空
		 * @param defauleValue
		 *            默认值
		 * @return
		 */
		public static Column createVarcharColumn(String name, int length, boolean isNull, String defauleValue) {
			Column column = new Column();
			column.name = name;
			column.length = length;
			column.isNull = isNull;
			column.columnType = ColumnType.VARCHAR;
			column.defauleValue = defauleValue != null && defauleValue.length() > 0 ? "'" + defauleValue + "'" : null;
			column.isAutoIncrement = false;
			return column;
		}

		/**
		 * 创建一个int类型的列，列长度默认为10
		 * 
		 * @param name
		 *            列名称
		 * @param isNull
		 *            是否为空
		 * @param defauleValue
		 *            列的默认值
		 * @return
		 */
		public static Column createIntColumn(String name, boolean isNull, Integer defauleValue) {
			Column column = new Column();
			column.name = name;
			column.length = Column.INT_DEFAULT_LENGTH;
			column.isNull = isNull;
			column.columnType = ColumnType.INT;
			column.defauleValue = defauleValue != null ? String.valueOf(defauleValue) : null;
			column.isAutoIncrement = false;
			return column;
		}

		/**
		 * 创建一个具有自增特性的int类型的列,列长度默认是10,默认不能为空<br>
		 * 请注意，创建了此类型字段的表格，此字段默认会成为主键中的一个
		 * 
		 * @param name
		 *            列名称
		 * @return
		 */
		public static Column createAutoIncrementIntColumn(String name) {
			Column column = new Column();
			column.name = name;
			column.length = Column.INT_DEFAULT_LENGTH;
			column.isNull = true;
			column.columnType = ColumnType.INT;
			column.defauleValue = null;
			column.isAutoIncrement = true;
			return column;
		}

		/**
		 * 创建一个text类型的列字段
		 * 
		 * @param name
		 *            字段名称
		 * @param isNull
		 *            是否为空
		 * @return
		 */
		public static Column createTextColumn(String name, boolean isNull) {
			Column column = new Column();
			column.name = name;
			column.isNull = isNull;
			column.columnType = ColumnType.TEXT;
			column.defauleValue = null;
			column.length = null;
			column.isAutoIncrement = false;
			return column;
		}

		/**
		 * 创建一个日期datime类型的列字段，当列字段有默认值时，会自动选用timestamp,无需调用者担心
		 * 
		 * @param name
		 *            列名称
		 * @param isNull
		 *            是否为空
		 * @param defauleValue
		 *            默认内容
		 * @return
		 */
		public static Column createDateTimeColumn(String name, boolean isNull, String defauleValue) {
			Column column = new Column();
			column.name = name;
			column.isNull = isNull;
			if (defauleValue != null && defauleValue.length() > 0) {
				column.columnType = ColumnType.TIMESTAMP;
				column.isNull = true;
			} else {
				column.columnType = ColumnType.DATETIME;
			}
			column.defauleValue = defauleValue != null && defauleValue.length() > 0 ? defauleValue : null;
			column.length = null;
			column.isAutoIncrement = false;
			return column;
		}

		/**
		 * 创建一个Long类型的列，根据MYSQL特性，会自动启用BIGINT，默认长度63位
		 * 
		 * @param name
		 *            列字段名称
		 * @param isNull
		 *            是否为空
		 * @param defauleValue
		 *            默认值
		 * @return
		 */
		public static Column createLongColumn(String name, boolean isNull, Long defauleValue) {
			Column column = new Column();
			column.name = name;
			column.length = Column.LONG_DEFAULT_LENGTH;
			column.isNull = isNull;
			column.columnType = ColumnType.LONG;
			column.defauleValue = defauleValue != null ? String.valueOf(defauleValue) : null;
			column.isAutoIncrement = false;
			return column;
		}

		/**
		 * 创建一个double类型的列字段
		 * 
		 * @param name
		 *            列名称
		 * @param isNull
		 *            是否为空
		 * @param defauleValue
		 *            默认值
		 * @return
		 */
		public static Column createDoubleColumn(String name, boolean isNull, Double defauleValue) {
			Column column = new Column();
			column.name = name;
			column.length = null;
			column.isNull = isNull;
			column.columnType = ColumnType.DOUBLE;
			column.defauleValue = defauleValue != null ? String.valueOf(defauleValue) : null;
			column.isAutoIncrement = false;
			return column;
		}

		/**
		 * 以SQL语句的方式展示一个表结构
		 */
		public String toString() {

			StringBuilder stringBuilder = new StringBuilder();

			// Step 1.拼接字段名及类型
			stringBuilder.append("`").append(this.getName()).append("` ").append(this.getColumnType().getValue());

			// Step 2.拼接类型长度
			if (this.getLength() != null) {
				stringBuilder.append("(").append(this.getLength()).append(")");
			}

			// Step 3.拼接是否为空
			if (!this.isNull()) {
				stringBuilder.append(" not null");
			}

			// Step 4.拼接默认值
			if (this.defauleValue != null && this.defauleValue.length() > 0) {
				stringBuilder.append((" default " + this.defauleValue));
			}

			// Step 5.拼接是否自增
			if (this.isAutoIncrement) {
				stringBuilder.append(" auto_increment");
			}

			return stringBuilder.toString();
		}

		/**
		 * 覆盖此方法的目的在于进行数据库两张表中的列格式对比
		 */
		@Override
		public boolean equals(Object obj) {

			if (obj == null) {
				return false;
			}

			if (this == obj) {
				return true;
			}

			if (this.getClass() != obj.getClass()) {
				return false;
			}

			if (!(obj instanceof Column)) {
				return false;
			}

			Column column = (Column) obj;

			if (this.name != null) {
				if (!this.name.equalsIgnoreCase(column.name)) {
					return false;
				}
			} else if (column.name != null) {
				return false;
			}

			if (this.columnType != column.columnType) {
				return false;
			}

			if (this.length != null) {
				if (!this.length.equals(column.length)) {
					return false;
				}
			}

			if (this.isNull != column.isNull) {
				return false;
			}

			if (this.defauleValue != null) {
				if (!this.defauleValue.equalsIgnoreCase(column.defauleValue)) {
					return false;
				}
			} else if (column.defauleValue != null) {
				return false;
			}

			if (this.isAutoIncrement != column.isAutoIncrement) {
				return false;
			}

			return true;
		}

		/**
		 * 因为{@link Object#equals()}方法被覆盖了，为了防止{@link Column}
		 * 被放置在Map中找不到对象，此处也覆盖了{@link Object#hashCode()}方法
		 */
		@Override
		public int hashCode() {
			int result = 17;
			result = 31 * result + (name != null ? name.hashCode() : 0);
			result = 31 * result + (length != null ? length.hashCode() : 0);
			result = 31 * result + (isNull ? 1 : 0);
			result = 31 * result + (columnType != null ? columnType.getValue().hashCode() : 0);
			result = 31 * result + (defauleValue != null ? defauleValue.hashCode() : 0);
			result = 31 * result + (isAutoIncrement ? 1 : 0);
			return result;
		}

		/**
		 * 根据结果集对象的当前内容创建一个列
		 * 
		 * @param rs
		 * @return
		 * @throws SQLException
		 */
		public static Column valueOf(ResultSet rs) throws SQLException {
			int columnType = rs.getInt("DATA_TYPE");
			switch (columnType) {

			case java.sql.Types.INTEGER:
				// 如果是主键,则创建int主键列，否则创建普通的int列
				if (rs.getString("IS_AUTOINCREMENT") != null
						&& rs.getString("IS_AUTOINCREMENT").equalsIgnoreCase("YES")) {
					return Column.createAutoIncrementIntColumn(rs.getString("COLUMN_NAME"));
				} else {
					return Column.createIntColumn(
							rs.getString("COLUMN_NAME"),
							rs.getInt("NULLABLE") == 0 ? false : true,
							rs.getString("COLUMN_DEF") != null && rs.getString("COLUMN_DEF").length() > 0 ? Integer
									.valueOf(rs.getString("COLUMN_DEF")) : null);
				}

			case java.sql.Types.VARCHAR:
				return Column.createVarcharColumn(rs.getString("COLUMN_NAME"), rs.getInt("COLUMN_SIZE"),
						rs.getInt("NULLABLE") == 0 ? false : true, rs.getString("COLUMN_DEF"));

			case java.sql.Types.LONGVARCHAR:
				return Column.createTextColumn(rs.getString("COLUMN_NAME"), rs.getInt("NULLABLE") == 0 ? false : true);

			case java.sql.Types.BIGINT:
				return Column.createLongColumn(
						rs.getString("COLUMN_NAME"),
						rs.getInt("NULLABLE") == 0 ? false : true,
						rs.getString("COLUMN_DEF") != null && rs.getString("COLUMN_DEF").length() > 0 ? Long.valueOf(rs
								.getString("COLUMN_DEF")) : null);

			case java.sql.Types.DOUBLE:
				return Column.createDoubleColumn(
						rs.getString("COLUMN_NAME"),
						rs.getInt("NULLABLE") == 0 ? false : true,
						rs.getString("COLUMN_DEF") != null && rs.getString("COLUMN_DEF").length() > 0 ? Double
								.valueOf(rs.getString("COLUMN_DEF")) : null);

			case java.sql.Types.TIMESTAMP:
				return Column.createDateTimeColumn(rs.getString("COLUMN_NAME"), rs.getInt("NULLABLE") == 0 ? false
						: true, rs.getString("COLUMN_DEF"));

			default:
				break;
			}
			return null;
		}

		public String getName() {
			return name;
		}

		public ColumnType getColumnType() {
			return columnType;
		}

		public Integer getLength() {
			return length;
		}

		public boolean isNull() {
			return isNull;
		}

		public String getDefauleValue() {
			return defauleValue;
		}

		public boolean isAutoIncrement() {
			return isAutoIncrement;
		}

		/**
		 * 为防止Column被其他类破坏规则，此处仅允许Table对他进行修改
		 * 
		 * @param isNull
		 */
		private void setNull(boolean isNull) {
			this.isNull = isNull;
		}

		/**
		 * 为了保证{@link Column}
		 * 相对于数据库表的约束性是相同的，因此会隐藏部分数据，防止该对象的数据被篡改，但是这样会影响到类的创建者无法看到类中的数据，
		 * 也就无法检查此类是否适合创建数据库表 基于这个原因这里提供了一个方法， 用以在提交时检验此对象是否符合数据库列要求<br>
		 * {@Column }对象是否符合数据库表结构的列要求
		 * 
		 * @return
		 */
		public boolean check() {

			if (name == null || name.length() == 0) {
				return false;
			}
			if (columnType == null) {
				return false;
			}

			return true;
		}
	}

	/**
	 * 数据库字段类型<br>
	 * 以后如果添加一种全新的类型时需要进行以下三步：<br>
	 * 1.在此枚举处增加想要的类型<br>
	 * 2.在<code>Column</code>类中加一个此类型的静态构造方法<br>
	 * 3.在<code>Column</code>类中的valueOf()方法中增加一个此类型由数据库<code>ResultSet</code>
	 * 对象如何转为Column对象的代码段
	 * 
	 * @author Lv.Mingwei
	 * 
	 */
	enum ColumnType {
		INT("int"), LONG("bigint"), DOUBLE("double"), DATETIME("datetime"), TIMESTAMP("timestamp"), VARCHAR("varchar"), TEXT(
				"text");

		private String value;

		private ColumnType(String value) {
			this.value = value;
		}

		public String getValue() {
			return this.value;
		}
	}
}
