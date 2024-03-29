package mydbutils;


import java.lang.reflect.InvocationTargetException;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.dbutils.ResultSetHandler;

public class MyBeanListHandler<T> implements ResultSetHandler<List<T>> {

	private Class clazz;
	public MyBeanListHandler(Class clazz) {
		this.clazz = clazz;
	}

	 // 方法一: 遍历 每个 ResultSetMetaData 元数据的 属性名， 然后根据属性名 匹配 PropertyDescriptor 对象
		// 将ResultSet中的数据 封装到 List 中
	@Override
	public List<T> handle(ResultSet resultset) throws SQLException {
		List<T> list= new ArrayList<>();


		ResultSetMetaData rsmd = resultset.getMetaData();
		int columnCount = rsmd.getColumnCount();
		try {
			BeanInfo beanInfo = Introspector.getBeanInfo(clazz);
			PropertyDescriptor[] pds = beanInfo.getPropertyDescriptors();
			while(resultset.next()){
				@SuppressWarnings("unchecked")
				T t = (T) clazz.newInstance();
				for(int i=1;i<=columnCount;i++){
					String columnName = rsmd.getColumnName(i);
//					System.out.println("columnName=>"+columnName);
					for(PropertyDescriptor pd:pds){
						if(pd.getName().equals(columnName)){
//							System.out.println("方法名==>"+pd.getName());
							Object value = resultset.getObject(i);
//							System.out.println(value);
							Method method= pd.getWriteMethod();
							method.invoke(t,value );
							break;
						}
					}
				}
				list.add(t);
			}
		} catch (IntrospectionException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		}


		return list;
	}




	  	//方法二、 通过遍历 PropertyDescriptor 数组，获取每一个属性名，再获取匹配的 resultSet 对应的列
		// 注意 : 每个 PropertyDescriptor 中都有一个额外的属性 class
	@Override
	public List<T> handle(ResultSet resultset) throws SQLException {
		List<T> list = new ArrayList<>();


		try {
			BeanInfo beanInfo =  Introspector.getBeanInfo(clazz);
			PropertyDescriptor[] pds = beanInfo.getPropertyDescriptors();
			while(resultset.next()){
				@SuppressWarnings("unchecked")
				T t = (T) clazz.newInstance();

				if(pds!=null&pds.length>1){
					for(PropertyDescriptor pd:pds){
						String propertyName = pd.getName();

						if(!propertyName.equals("class")){// 排除 属性名是 class 时，获取不到 值对象
							Object value =resultset.getObject(propertyName);
							Method method = pd.getWriteMethod();
							method.invoke(t,value);
						}
					}
					list.add(t);
				}
			}

		} catch (IntrospectionException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}

		return list;
	}


	// 方法三 , 通过BeanUtils 实现数据封装
	@Override
	public List<T> handle(ResultSet resultset) throws SQLException {

		List<T> list = new ArrayList<>();
		ResultSetMetaData rsmd = resultset.getMetaData();
		int count = rsmd.getColumnCount();

		while(resultset.next()){
			try {
				T t = (T)clazz.newInstance();
				for(int i=1;i<=count;i++){
					String propertyName= rsmd.getColumnName(i);
					Object value  = resultset.getObject(propertyName);
//					String value = resultset.getString(propertyName);   // 数据库中所有的类型都可以通过 getString()获得
					System.out.println(propertyName+"==>"+value);
					BeanUtils.setProperty(t,propertyName, value);
				}
				list.add(t);
			} catch (InstantiationException | IllegalAccessException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}
		}
		return list;

	}

	// 方法四:  BeanUtils 的 populate 方法

	@Override
	public List<T> handle(ResultSet resultset) throws SQLException {
		List<T> list= new ArrayList<>();
		ResultSetMetaData rsmd = resultset.getMetaData();
		int count = rsmd.getColumnCount();
		try {
			while(resultset.next()){
				T t = (T) clazz.newInstance();
//				一个对象一个map对象
				Map<String,Object[]> map = new HashMap<>();
				for(int i=1;i<=count;i++){
					String propertyName = rsmd.getColumnName(i);
					Object[] objs = new Object[]{resultset.getObject(propertyName)};
					map.put(propertyName,objs);
				}
				BeanUtils.populate(t, map);
				list.add(t);
			}
		} catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
			e.printStackTrace();
		}

		return list;
	}
}
