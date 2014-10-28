

/********************************************************************
 ** This software (or technical data) was produced for the U. S.
 ** Government under contract 2009-0917826-016 and is subject to 
 ** the Rights in Data-General Clause 52.227-14. Alt. IV (DEC 2007).
 ** 
 ** (c) Copyright 2012 The MITRE Corporation. All Rights Reserved.
 ********************************************************************/

package stonesoup;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import org.hibernate.annotations.Type;

@Entity
@DiscriminatorValue(value = "1")
public class PendingTransactions 
{
	@Id
    @GeneratedValue(strategy = GenerationType.AUTO)
	public int id;
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	@Column
	public String trans_from;
	@Column
	public String trans_to;
	@Column
	public int amount;
	@Column
	public String trans_date;
	
	public String getTrans_from() {
		return trans_from;
	}
	public void setTrans_from(String trans_from) {
		this.trans_from = trans_from;
	}
	public String getTrans_to() {
		return trans_to;
	}
	public void setTrans_to(String trans_to) {
		this.trans_to = trans_to;
	}
	public int getAmount() {
		return amount;
	}
	public void setAmount(int amount) {
		this.amount = amount;
	}
	public String getTrans_date() {
		return trans_date;
	}
	public void setTrans_date(String trans_date) {
		this.trans_date = trans_date;
	}
	
	
}
