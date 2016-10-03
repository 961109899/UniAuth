package com.dianrong.common.uniauth.server.data.mapper;

import com.dianrong.common.uniauth.server.data.entity.Domain;
import com.dianrong.common.uniauth.server.data.entity.DomainExample;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface DomainMapper {
    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table domain
     *
     * @mbggenerated Tue Jan 26 11:13:56 CST 2016
     */
    int countByExample(DomainExample example);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table domain
     *
     * @mbggenerated Tue Jan 26 11:13:56 CST 2016
     */
    int deleteByExample(DomainExample example);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table domain
     *
     * @mbggenerated Tue Jan 26 11:13:56 CST 2016
     */
    int deleteByPrimaryKey(Integer id);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table domain
     *
     * @mbggenerated Tue Jan 26 11:13:56 CST 2016
     */
    int insert(Domain record);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table domain
     *
     * @mbggenerated Tue Jan 26 11:13:56 CST 2016
     */
    int insertSelective(Domain record);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table domain
     *
     * @mbggenerated Tue Jan 26 11:13:56 CST 2016
     */
    List<Domain> selectByExample(DomainExample example);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table domain
     *
     * @mbggenerated Tue Jan 26 11:13:56 CST 2016
     */
    Domain selectByPrimaryKey(Integer id);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table domain
     *
     * @mbggenerated Tue Jan 26 11:13:56 CST 2016
     */
    int updateByExampleSelective(@Param("record") Domain record, @Param("example") DomainExample example);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table domain
     *
     * @mbggenerated Tue Jan 26 11:13:56 CST 2016
     */
    int updateByExample(@Param("record") Domain record, @Param("example") DomainExample example);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table domain
     *
     * @mbggenerated Tue Jan 26 11:13:56 CST 2016
     */
    int updateByPrimaryKeySelective(Domain record);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table domain
     *
     * @mbggenerated Tue Jan 26 11:13:56 CST 2016
     */
    int updateByPrimaryKey(Domain record);
}