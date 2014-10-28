

/********************************************************************
 ** This software (or technical data) was produced for the U. S.
 ** Government under contract 2009-0917826-016 and is subject to 
 ** the Rights in Data-General Clause 52.227-14. Alt. IV (DEC 2007).
 ** 
 ** (c) Copyright 2012 The MITRE Corporation. All Rights Reserved.
 ********************************************************************/

package stonesoup;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

import stonesoup.studentmodel.Config;
import stonesoup.studentmodel.Course;
import stonesoup.studentmodel.DataConnection;
import stonesoup.studentmodel.Registration;
import stonesoup.studentmodel.Student;
import stonesoup.studentmodel.Teacher;

// A simple DAO for accessing the data for the registration application.
public class RegistrationDao {

	// Constructor
	public RegistrationDao() {
	}


    /**
	 * Adds a new Registration to the db. This expects both a Student and a
	 * Course object associated with the Registration or it will thrown an
	 * IllegalArgumentException as an unchecked error.
     * The registration will only be allowed if the student and the course both
     * exist and there is no enrollment conflict.
     *
     * @param registration The registration
     * @throws SQLException on error
	 */
	public void addRegistration(final Config config) throws SQLException {
        final Student regstudent = getStudent(config.getStudentId());
        final Course intocourse = getCourseByCourseNumber(config.getCourseNumber());
        final Registration reg = new Registration(regstudent, intocourse);

		// first confirm that it is OK to enroll
		final Student confirmStudent = this.getStudent(regstudent.getId());
		if (confirmStudent == null) {
			throw new IllegalArgumentException("Student doesn't exist");
		}

		final Course confirmCourse = this.getCourse(intocourse.getId());
		if (confirmCourse == null) {
			throw new IllegalArgumentException("Course doesn't exist");
		}

		if (confirmCourse.isFull()) {
			throw new IllegalStateException("Course is full");
		}
		Connection conn = null;
		try {
			conn = new DataConnection().initialize();
			PreparedStatement stmt = null;
			try {
				stmt = conn.prepareStatement("INSERT INTO registration (student_id, course_id) VALUES (?, ?)");
				stmt.setInt(1, reg.getStudent().getId());
				stmt.setInt(2, reg.getCourse().getId());
				stmt.executeUpdate();
			} finally {
				closeStatement(stmt);
			}
		} finally {
			conn.close();
		}
		regstudent.toString();
	}

    /**
     * Lists the Courses student is registered for.
     *
     * @param config with student, password set
     * @return the list of courses
     * @throws SQLException on error
     */
	public Set<Course> getRegistrationsForStudent(final Config config) throws SQLException {
		final Student studentreg = getStudent(config.getStudentId());
		final Set<Course> courses = new HashSet<Course>();

		Connection conn = null;
		PreparedStatement stmt = null, stmt2 = null, stmt3 = null;
		ResultSet rs = null, rs2 = null, rs3 = null;

		try {
			//good  password check
			if (!passwordCheck(studentreg, config.getPassword())) {
				System.out.println("Incorrect Password");
			} else {

		    conn = new DataConnection().initialize();
			stmt = conn
					.prepareStatement("SELECT * FROM course c "
							+ "JOIN registration r on "
							+ "r.course_id = c.id WHERE r.student_id = ?");
			stmt.setInt(1, studentreg.getId());
			rs = stmt.executeQuery();
			while (rs.next()) {
				final int id = rs.getInt(1);
				final String name = rs.getString(2);
				final int limit = rs.getInt(3);
				final String courseNumber = rs.getString(4);
				final boolean isOffered = rs.getBoolean(5);
				final int tid = rs.getInt(6);


				String fname = null;
				String lname = null;
				stmt3=conn.prepareStatement("Select firstname, lastname from teacher where id = ?");
				stmt3.setInt(1, tid);
				rs3 = stmt3.executeQuery();
				while (rs3.next()) {
					fname = rs3.getString(1);
					lname = rs3.getString(2);
				}

				final Course course = new Course(id, name, limit, courseNumber);
				course.setOffered(isOffered);
				course.setTeacher(new Teacher(tid,fname,lname));

				// Need to calculate the total number of students
				// enrolled in the course as well. Probably could do this
				// as part of the first query, but this works.
				stmt2 = conn
						.prepareStatement("SELECT count(*) FROM registration WHERE course_id = ?");
				stmt2.setInt(1, id);
				rs2 = stmt2.executeQuery();
				while (rs2.next()) {
					final int totalEnrolled = rs2.getInt(1);
					course.setTotalEnrolled(totalEnrolled);
				}
				courses.add(course);
			}
			}
		} finally {
			closeResultSet(rs);
			closeResultSet(rs2);
			closeStatement(stmt);
			closeStatement(stmt2);
			conn.close();
		}
		return courses;
	}

	/**
	 * Deletes the registration
	 * @param config
	 * @throws SQLException on error
	 */
	public void deleteRegistration(final Config config)	throws SQLException {
    	final Student unregstudent = getStudent(config.getStudentId());
    	final Course outofcourse = getCourseByCourseNumber(config.getCourseNumber());

		Connection conn = null;
		PreparedStatement stmt = null;
		try {
			conn = new DataConnection().initialize();
			if (!passwordCheck(unregstudent, config.getPassword())) {
				System.out.println("Incorrect Password");
			} else {
				stmt = conn.prepareStatement("DELETE FROM registration WHERE "
						+"student_id = ? AND course_id = ?");
				stmt.setInt(1, unregstudent.getId());
				stmt.setInt(2, outofcourse.getId());
				stmt.executeUpdate();
		        System.out.println(unregstudent.toString()+" has been deleted from "+outofcourse.toString());
			}
		} finally {
			closeStatement(stmt);
			conn.close();
		}
	}

	/*
	 * Gets a list of all the courses
	 * @return The courses
	 * @throws SQLException on error
	 */
	public Set<Course> listCourses() throws SQLException {

		final Set<Course> courses = new HashSet<Course>();
		Connection conn = null;
		conn = new DataConnection().initialize();
		PreparedStatement stmt = null, stmt2 = null, stmt3 = null;
		ResultSet rs = null, rs2 = null, rs3 = null;

		try {
			stmt = conn
					.prepareStatement("SELECT c.id, c.name, c.size_limit, c.course_number, "
							+ "c.is_offered, t.id, t.firstname, t.lastname "
							+ "FROM course c LEFT JOIN teacher t ON c.teacher_id = t.id");
			rs = stmt.executeQuery();
			while (rs.next()) {
				final int courseId = rs.getInt(1);
				final String name = rs.getString(2);
				final int size = rs.getInt(3);
				final String courseNumber = rs.getString(4);
				final boolean isOffered = rs.getBoolean(5);
				final Course course = new Course(courseId, name, size,
						courseNumber);
				course.setOffered(isOffered);
				final int teacherId = rs.getInt(6);
				final String firstName = rs.getString(7);
				final String lastName = rs.getString(8);
				if (teacherId > 0) {
					final Teacher teacher = new Teacher(teacherId, firstName,
							lastName);
					course.setTeacher(teacher);
				}

				// Need to calculate the total number of students
				// enrolled in the course as well. Probably could do this
				// as part of the first query, but this works.
				stmt2 = conn
						.prepareStatement("SELECT count(*) FROM registration WHERE course_id = ?");
				stmt2.setInt(1, courseId);
				rs2 = stmt2.executeQuery();
				while (rs2.next()) {
					final int totalEnrolled = rs2.getInt(1);
					course.setTotalEnrolled(totalEnrolled);
				}

				// get the prereqs as well
				stmt3 = conn
						.prepareStatement("SELECT pre.id, pre.name, pre.size_limit, pre.course_number "
								+ "FROM course c "
								+ "JOIN prereq_course map ON map.course_id = c.id "
								+ "JOIN course pre ON map.prereq_course_id = pre.id "
								+ "where c.id = ?");
				stmt3.setInt(1, course.getId());
				rs3 = stmt3.executeQuery();
				while (rs3.next()) {
					final int preCourseId = rs3.getInt(1);
					final String preName = rs3.getString(2);
					final int preSize = rs3.getInt(3);
					final String preCourseNumber = rs3.getString(4);
					final Course prereq = new Course(preCourseId, preName,
							preSize, preCourseNumber);
					final Set<Course> prereqs = new HashSet<Course>();
					prereqs.add(prereq);
					course.setPrereqs(prereqs);
				}

				courses.add(course);
			}
		} finally {
			closeResultSet(rs);
			closeResultSet(rs2);
			closeResultSet(rs3);
			closeStatement(stmt);
			closeStatement(stmt2);
			closeStatement(stmt3);
			conn.close();
		}

		return courses;
	}

    /**
     * Gets all students
     *
     * @return list of all students
     * @throws SQLException on error
     */
	public Set<Student> listStudents() throws SQLException {
		final Set<Student> students = new HashSet<Student>();
		Connection conn = null;
		try {
			conn = new DataConnection().initialize();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        PreparedStatement stmt = null;
		ResultSet rs = null;

		try {
			stmt = conn
					.prepareStatement("SELECT id, firstname, lastname FROM student ORDER BY id;");
			stmt.executeQuery();
			rs = stmt.getResultSet();
			while (rs.next()) {
				final int id = rs.getInt(1);
				final String first = rs.getString(2);
				final String last = rs.getString(3);
				final Student student = new Student(id, first, last);
				students.add(student);
			}
		} finally {
			closeResultSet(rs);
        	closeStatement(stmt);
			conn.close();
		}
		return students;
	}

    /**
     * Gets a student by id.
     * Throws unchecked error when invalid student ID is used.
     *
     * @param id The id to search for
     * @return The student
     * @throws SQLException on error
     */
	private Student getStudent(final int studentID) throws SQLException {
		Student s = null;
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			conn = new DataConnection().initialize();
			stmt = conn.prepareStatement("SELECT * FROM student WHERE id = ?");
			stmt.setInt(1, studentID);
			rs = stmt.executeQuery();
			while (rs.next()) {
				final String first = rs.getString("firstname");
				final String last = rs.getString("lastname");
				s =  new Student(studentID, first, last);
			}
		} finally {
			closeResultSet(rs);
			closeStatement(stmt);
			conn.close();
		}
		return s;
	}

	@SuppressWarnings("unused")
	private Student getStudentByName(String fname, String lname, String pwd) throws SQLException {
		Connection conn = null;
		conn = new DataConnection().initialize();
		Statement stmt = null;
		ResultSet rs = null;
		int id = 0;
		String fn = null, ln = null, pass = null;

		//Vulnerable
		try {
			String SQL = "SELECT id,firstname,lastname FROM student WHERE firstname ='"
					+ fname
					+ "' and lastname='"
					+ lname
					+ "' and password='"
					+ pwd + "'";
			//System.out.println(SQL);
			stmt = (Statement) conn.createStatement();
			rs = stmt.executeQuery(SQL);

			//getRSDisplay(rs); // Information Leak
			rs.first();
			id = rs.getInt(0);
			fn = rs.getString(fn);
			ln = rs.getString(ln);
			pass = rs.getString(pass);
		} finally {
			closeResultSet(rs);
			conn.close();
		}
		return new Student(id, fn, ln, pass); // if query is returning more
		// than one row
		// b/c duplicate users are
		// not checked before
		// inserting
	}

	/**
     * Gets a Course by id.
     * Throws an unchecked error when an invalid course ID is used.
     *
     * @param id The id to search for
     * @return The course
     * @throws SQLException on error
     */
	private Course getCourse(final int courseid) throws SQLException {
		Course course = null;

		Connection conn = null;
		PreparedStatement stmt = null, stmt2 = null;
		ResultSet rs = null, rs2 = null;
		try {
			conn = new DataConnection().initialize();
			stmt = conn.prepareStatement("SELECT * FROM course WHERE id = ?");
			stmt.setInt(1, courseid);
			rs = stmt.executeQuery();
			while (rs.next()) {
				final String name = rs.getString("name");
				final int limit = rs.getInt("size_limit");
				final String courseNumber = rs.getString("course_number");
				final boolean isOffered = rs.getBoolean("is_offered");
				course = new Course(courseid, name, limit, courseNumber);
				course.setOffered(isOffered);

				// Need to calculate the total number of students
				// enrolled in the course as well. Probably could do this
				// as part of the first query, but this works.
				stmt2 = conn
						.prepareStatement("SELECT count(*) FROM registration WHERE course_id = ?");
				stmt2.setInt(1, courseid);
				rs2 = stmt2.executeQuery();
				while (rs2.next()) {
					final int totalEnrolled = rs2.getInt(1);
					course.setTotalEnrolled(totalEnrolled);
				}
			}
		} finally {
			closeResultSet(rs);
			closeResultSet(rs2);
			closeStatement(stmt);
			closeStatement(stmt2);
			conn.close();
		}

		return course;
	}

	/**
	 * Gets a Course by course NAME.
	 * Throws an unchecked error when an invalid
	 * course number is used.
	 *
	 * @param string courseNumber The courseNumber to search for
	 * @return The course
	 * @throws SQLException  on error
	 */
	private Course getCourseByCourseNumber(final String courseNumber) throws SQLException {
		Course course = null;
		Connection conn = null;
		PreparedStatement stmt = null, stmt2 = null;
		ResultSet rs = null, rs2 = null;
		try {
			conn = new DataConnection().initialize();
			stmt = conn.prepareStatement("SELECT * FROM course WHERE course_number = ?");
			stmt.setString(1, courseNumber);
			rs = stmt.executeQuery();
			while (rs.next()) {
				final int id = rs.getInt("id");
				final String name = rs.getString("name");
				final int limit = rs.getInt("size_limit");
				final boolean isOffered = rs.getBoolean("is_offered");
				course = new Course(id, name, limit, courseNumber);
				course.setOffered(isOffered);

				// Need to calculate the total number of students
				// enrolled in the course as well. Probably could do this
				// as part of the first query, but this works.
				stmt2 = conn.prepareStatement("SELECT count(*) FROM registration WHERE course_id = ?");
				stmt2.setInt(1, id);
				rs2 = stmt2.executeQuery();
				while (rs2.next()) {
					final int totalEnrolled = rs2.getInt(1);
					course.setTotalEnrolled(totalEnrolled);
				}
			}
		} finally {
			closeResultSet(rs);
			closeResultSet(rs2);
			closeStatement(stmt);
			closeStatement(stmt2);
			conn.close();
		}

		return course;
	}

    /**
     * Creates a new Teacher
     * @param config object
     * @throws SQLException on error
     */
	public void createTeacher(final Config config) throws SQLException {
		final Teacher newteacher = new Teacher(config.getFirstName(), config.getLastName());
		if (newteacher == null || (newteacher.getFirstName() == null)
				|| (newteacher.getLastName() == null)) {
			throw new IllegalArgumentException("Invalid new teacher data specified");
		}
		Connection conn = null;
		conn = new DataConnection().initialize();
		PreparedStatement stmt = null;
		ResultSet rs = null;

		try {
			stmt = conn
					.prepareStatement("INSERT INTO teacher (firstname, lastname) VALUES (?,?)");
			stmt.setString(1, newteacher.getFirstName());
			stmt.setString(2, newteacher.getLastName());
			stmt.executeUpdate();

			// grab auto-gen'd key
			rs = stmt.getGeneratedKeys();
			while (rs.next()) {
				// should just be one
				final int id = rs.getInt(1);
				newteacher.setId(id);
			}
		} finally {
			closeResultSet(rs);
			closeStatement(stmt);
			conn.close();
		}
        System.out.println("Teacher created: "+newteacher.toString());
	}

    /**
     *
     * @return list of all Teacher
     * @throws SQLException on error
     */
	public Set<Teacher> listTeachers() throws SQLException {
		final Set<Teacher> teachers = new HashSet<Teacher>();
		Connection conn = null;
		try {
			conn = new DataConnection().initialize();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        PreparedStatement stmt = null;
		ResultSet rs = null;

		try {
			stmt = conn
					.prepareStatement("SELECT id, firstname, lastname FROM teacher ORDER BY id;");
			stmt.executeQuery();
			rs = stmt.getResultSet();
			while (rs.next()) {
				final int id = rs.getInt(1);
				final String first = rs.getString(2);
				final String last = rs.getString(3);
				final Teacher teacher = new Teacher(id, first, last);
				teachers.add(teacher);
			}
		} finally {
			closeResultSet(rs);
        	closeStatement(stmt);
			conn.close();
		}
		return teachers;
	}

	/**
     * Creates a new student
     * @param config object
     * @throws SQLException on error
     */
	public void createStudent(final Config config) throws SQLException {
		final Student newstudent = new Student(config.getFirstName(), config.getLastName(), config.getPassword());

		Connection conn = null;
		try {
			conn = new DataConnection().initialize();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		PreparedStatement stmt = null;
		ResultSet rs = null;

		try {
			stmt = conn
					.prepareStatement("INSERT INTO student (firstname, lastname, password) VALUES (?,?,?)");
			stmt.setString(1, newstudent.getFirstName());
			stmt.setString(2, newstudent.getLastName());
			stmt.setString(3, newstudent.getPassword());
			stmt.executeUpdate();

			// grab auto-gen'd key
			rs = stmt.getGeneratedKeys();
			while (rs.next()) {
				// should just be one
				final int id = rs.getInt(1);
				newstudent.setId(id);
			}
		} finally {
			closeResultSet(rs);
			closeStatement(stmt);
			conn.close();
		}
        System.out.println("Student created: "+newstudent.toString());
	}

	/**
	 * Deletes the specified student
	 * @param config object
	 * @throws SQLException on error
	 */
	public void deleteStudent(final Config config) throws SQLException {
		Connection conn = null;
		try {
			conn = new DataConnection().initialize();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		PreparedStatement stmt = null;
		try {
			stmt = conn.prepareStatement("DELETE FROM student WHERE id = ?");
			stmt.setInt(1, config.getStudentId());
			stmt.executeUpdate();
		} finally {
			closeStatement(stmt);
			conn.close();
		}
	}

	/**
	 * Deletes the specified teacher
	 * @param config object
	 * @throws SQLException on error
	 */
	public void deleteTeacher(final Config config) throws SQLException {
		Connection conn = null;
		try {
			conn = new DataConnection().initialize();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		PreparedStatement stmt = null;

		try {
			stmt = conn.prepareStatement("DELETE FROM teacher WHERE id = ?");
			stmt.setInt(1, config.getTeacherId());
			stmt.executeUpdate();
		} finally {
			closeStatement(stmt);
			conn.close();
		}
	}

    /**
     * Creates a new course.  Throws an unchecked error, if invalid data is
     * specified.
     *
     * @param course The course to create
     * @throws SQLException on error
     */
	public void createCourse(final Config config) throws SQLException {
     	final Course newcourse = new Course(config.getCourseName(), config.getSizeLimit(), config.getCourseNumber());

		if (newcourse == null || (newcourse.getName() == null)
				|| (newcourse.getCourseNumber() == null)
				|| (newcourse.getLimit() < 1)) {
			throw new IllegalArgumentException("Invalid course data specified");
		}

		Connection conn = null;
		try {
			conn = new DataConnection().initialize();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		PreparedStatement stmt = null;
		ResultSet rs = null;

		try {
			final StringBuilder builder = new StringBuilder();
			builder.append("INSERT INTO course (name, course_number, size_limit");
			final Teacher teacher = newcourse.getTeacher();
			if (teacher != null) {
				builder.append(", teacher_id) VALUES (?, ?, ?, ?");
			} else {
				builder.append(") VALUES (?, ?, ?)");
			}
			stmt = conn.prepareStatement(builder.toString());
			stmt.setString(1, newcourse.getName());
			stmt.setString(2, newcourse.getCourseNumber());
			stmt.setInt(3, newcourse.getLimit());
			if (teacher != null) {
				stmt.setInt(4, teacher.getId());
			}
			stmt.executeUpdate();

			// grab auto-gen'd key
			rs = stmt.getGeneratedKeys();
			while (rs.next()) {
				// should just be one
				final int id = rs.getInt(1);
				newcourse.setId(id);
			}
		} finally {
			closeResultSet(rs);
			closeStatement(stmt);
			conn.close();
		}
		System.out.println(newcourse.getId() + " ,has been created.");
	}

	/**
	 * Deletes the course
	 * @param course The course to delete
	 * @throws SQLException on error
	 */
	public void deleteCourse(final Config config) throws SQLException {

    	final Course deletcourse = getCourseByCourseNumber(config.getCourseNumber());
		if (deletcourse == null ) {
			throw new IllegalArgumentException("Invalid course data specified");
		}
		//TODO should have check to delete registered students

		Connection conn = null;
		try {
			conn = new DataConnection().initialize();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		PreparedStatement stmt = null;
		try {
			stmt = conn
					.prepareStatement("DELETE FROM course WHERE course_number = ?;");
			stmt.setString(1, deletcourse.getCourseNumber());
			stmt.executeUpdate();
			System.out.println(deletcourse.toString() + " has been deleted.");
		} finally {
			closeStatement(stmt);
			conn.close();
		}
	}

	/**
	 * Deletes the registration
	 * @param registration The registration to delete
	 * @throws SQLException on error
	 */
	public void deleteRegistration(Student student, final String courseNumber,
			final String password) throws SQLException {

		Connection conn = null;
		try {
			conn = new DataConnection().initialize();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		PreparedStatement stmt = null;
		try {
			// faulty password check
			if (!passwordCheck(student, password)) {
				System.err.println("Incorrect Password");
			} else {
				Course course = getCourseByCourseNumber(courseNumber);
				stmt = conn
						.prepareStatement("DELETE FROM registration WHERE student_id = ? AND course_id = ?");
				stmt.setInt(1, student.getId());
				stmt.setInt(2, course.getId());
				stmt.executeUpdate();
			}
		} finally {
			closeStatement(stmt);
			conn.close();
		}
	}

	// Vulnerable password check
	private boolean passwordCheck(Student unregstudent, String password) throws SQLException {
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
		try {
			conn = new DataConnection().initialize();
			stmt = conn.createStatement();
			String query = "SELECT * FROM student WHERE id=" + unregstudent.getId() + " AND password='" + password +"'";
			rs = stmt.executeQuery(query);
			if (rs.next()) {
				return true;
			} else {
				return false;
			}
		} catch (SQLException e) {
			System.err.println("passwordCheck sql error");
		} finally {
			rs.close();
			stmt.close();
			conn.close();
		}
		return false;
	}

	// good password check
	private boolean pwdgCheck(Student unregstudent, String password) throws SQLException {
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			conn = new DataConnection().initialize();
			// passing concatd str to incorrectly parameterized query
			stmt = conn.prepareStatement("SELECT * FROM student WHERE id=? AND password=?");
			stmt.setInt(1, unregstudent.getId());
			stmt.setString(2, password);

			rs = stmt.executeQuery();
			if (rs.next()) {
				return true;
			} else {
				return false;
			}
		} catch (SQLException e) {
			System.err.println("sql error");
		} finally {
			closeStatement(stmt);
			conn.close();
		}
		return false;
	}

	// DEBUGGER- vulnerable - accepts any SQL command
	public void debug(final Config config) throws SQLException {
		if (config.getInstructions() == null) {
			throw new IllegalArgumentException("Invalid data specified");
		}
		Connection conn = null;
		try {
			conn = new DataConnection().initialize();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			System.out.println("SQL Command:" + config.getInstructions());
			stmt = conn.prepareStatement(config.getInstructions());
			rs = stmt.executeQuery();
			getRSDisplay(rs);
		} finally {
			closeStatement(stmt);
			conn.close();
		}
	}


	private static void getRSDisplay(ResultSet rs) throws SQLException {
		// String tableName = rsMetaData.getTableName(i);
		ResultSetMetaData rsMetaData = rs.getMetaData();
		int numberOfColumns = rsMetaData.getColumnCount();
		//System.out.println("numberOfColumns:" + numberOfColumns);
		// get the row number of the last row which is also the row count
		rs.last();
		int rowCount = rs.getRow();
		//System.out.println("rowCount:" + rowCount);

		int j = 0;
		Object[][] qArray1;
		qArray1 = new Object[rowCount][numberOfColumns];
		rs.first();
		// TODO return a recordset w/ columnnames, and values
		while (rs.next()) {
			// get the column names; column indexes start from 1
			for (int i = 1; i <= numberOfColumns; i++) {
				System.out.println(rsMetaData.getColumnName(i) + " | "
						+ rs.getString(i));
				if (j == 0) {
					// Get the name of the column
					qArray1[0][i - 1] = rsMetaData.getColumnName(i);
				} else {
					qArray1[j][i - 1] = rs.getString(i);
				}
			}
			// row done, go to next one
			j++;
		}
	}

	public void printStudents(final Set<Student> list)
    {
        final StringBuilder builder = new StringBuilder();
        builder.append("Students Enrolled:\n");
        for (final Student student : list) {
            builder.append(student.toString()+"\n");
        }
        System.out.println(builder.toString());
    }

    public void printTeachers(final Set<Teacher> list)
    {
        final StringBuilder builder = new StringBuilder();
        builder.append("Teachers available:\n");
        for (final Teacher teacher : list) {
            builder.append(teacher.toString()+"\n");
        }
        System.out.println(builder.toString());
    }

    public void printRegistrations(final Set<Course> list)
    {
    	boolean listExists = false;
        final StringBuilder builder = new StringBuilder();
        for (final Course course : list)
        {
            builder.append("cID:"+course.getId() + " ");
		    final Teacher teacher = course.getTeacher();
	        builder.append(teacher.toString() + "\n");
		    listExists = true;
        }
        if (listExists){
        	System.out.println("Student Rgeg:" + builder.toString());
        }
    }

	public void printCourses(final Set<Course> list)
	    {
			boolean listExists = false;
	        final StringBuilder builder = new StringBuilder();
	        for (final Course course : list)
	        {
	            if (course.isOffered())
	            {
	                builder.append("CourseID: "+course.getId());

	                final Teacher teacher = course.getTeacher();
	                if(teacher != null)
	                {
	                    builder.append(", Teacher: "+teacher.toString());
	                }

	                builder.append(", Seats Open ")
	                        .append(course.getLimit() - course.getTotalEnrolled())
	                        .append(" - prerequisites: ");

	                for (final Course prereq : course.getPrereqs())
	                {
	                    builder.append(prereq.getId());
	                }

	                builder.append("\n");
	            }
	            listExists = true;
	        }
	        if (listExists){
	        	System.out.println("Courses available:\n"+builder.toString());
	        }
	 }

	/**
	 * Closes the prepared statement
	 * @param stmt The prepared statement to close
	 */
	private void closeStatement(PreparedStatement stmt) throws SQLException {
		if (stmt != null) {
			try {
				stmt.close();
			} catch (SQLException e) {
				System.err.println("Failure closing Statement");
			}
		}
	}

	/**
	 * Closes the result set
	 * @param rs The result set to close
	 */
	private void closeResultSet(ResultSet rs) throws SQLException {
		if (rs != null) {
			try {
				rs.close();
			} catch (SQLException e) {
				System.err.println("Failure closing ResultSet");
			}
		}
	}


}
