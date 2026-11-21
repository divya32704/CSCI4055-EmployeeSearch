/**
 * Author: Lon Smith, Ph.D. (template)
 * Completed by: Divya Shah, Daniel Darmey
 * Description: Loads departments and projects from a COMPANY-schema database and
 *    searches for employees in / not in the selected departments and/or projects.
 * Tools used: Eclipse IDE + WindowBuilder (GUI), MySQL Connector/J (JDBC driver), MySQL on AWS RDS.
 */

import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.JLabel;
import java.awt.Font;
import javax.swing.JTextField;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JCheckBox;
import javax.swing.ListSelectionModel;
import javax.swing.JOptionPane;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

public class EmployeeSearchFrame extends JFrame {

	private static final long serialVersionUID = 1L;

	// Connection settings: put your own login here to test, but DON'T commit your real password
	private static final String DB_HOST = "localhost";
	private static final String DB_PORT = "3306";
	private static final String DB_USER = "root";
	private static final String DB_PASSWORD = "";

	private JPanel contentPane;
	private JTextField txtDatabase;
	private String filledDatabase;
	private JList<String> lstDepartment;
	private DefaultListModel<String> department = new DefaultListModel<String>();
	private JList<String> lstProject;
	private DefaultListModel<String> project = new DefaultListModel<String>();
	private JTextArea textAreaEmployee;
	private JCheckBox chckbxNotDept;
	private JCheckBox chckbxNotProject;
	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					EmployeeSearchFrame frame = new EmployeeSearchFrame();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/** Opens a connection to the given database name. */
	private Connection getConnection(String dbName) throws SQLException {
		String url = "jdbc:mysql://" + DB_HOST + ":" + DB_PORT + "/" + dbName;
		return DriverManager.getConnection(url, DB_USER, DB_PASSWORD);
	}

	/** Fill: reset the Department and Project lists from the entered database. */
	private void fillLists() {
		String dbName = txtDatabase.getText().trim();

		filledDatabase = null;
		department.clear();
		project.clear();
		textAreaEmployee.setText("");

		try (Connection conn = getConnection(dbName);
				Statement stmt = conn.createStatement()) {

			try (ResultSet rs = stmt.executeQuery("SELECT Dname FROM DEPARTMENT ORDER BY Dname")) {
				while (rs.next()) {
					department.addElement(rs.getString("Dname"));
				}
			}
			try (ResultSet rs = stmt.executeQuery("SELECT Pname FROM PROJECT ORDER BY Pname")) {
				while (rs.next()) {
					project.addElement(rs.getString("Pname"));
				}
			}
			filledDatabase = dbName;
		} catch (SQLException ex) {
			JOptionPane.showMessageDialog(this, "The database \"" + dbName + "\" could not be opened.", "Database Error", JOptionPane.ERROR_MESSAGE);
		}
	}
	private void searchEmployees() {
		if (filledDatabase == null) {
			JOptionPane.showMessageDialog(this, "Fill a database before searching.", "Database Required",
					JOptionPane.WARNING_MESSAGE);
			return;
		}

		List<String> selectedDepartments = lstDepartment.getSelectedValuesList();
		List<String> selectedProjects = lstProject.getSelectedValuesList();
		StringBuilder sql = new StringBuilder("SELECT E.Fname, E.Lname FROM EMPLOYEE E");
		if (!selectedDepartments.isEmpty()) {
			String departmentMatch = chckbxNotDept.isSelected() ? "NOT EXISTS" : "EXISTS";
			sql.append(" WHERE ").append(departmentMatch)
					.append(" (SELECT 1 FROM DEPARTMENT D WHERE D.Dnumber = E.Dno AND D.Dname IN (");
			for (int i = 0; i < selectedDepartments.size(); i++) {
				if (i > 0) {
					sql.append(", ");
				}
				sql.append("?");
			}
			sql.append("))");
		}
		if (!selectedProjects.isEmpty()) {
			String projectMatch = chckbxNotProject.isSelected() ? "NOT EXISTS" : "EXISTS";
			sql.append(selectedDepartments.isEmpty() ? " WHERE " : " AND ");
			sql.append(projectMatch).append(" (SELECT 1 FROM WORKS_ON W JOIN PROJECT P ON P.Pnumber = W.Pno ")
					.append("WHERE W.Essn = E.Ssn AND P.Pname IN (");
			for (int i = 0; i < selectedProjects.size(); i++) {
				if (i > 0) {
					sql.append(", ");
				}
				sql.append("?");
			}
			sql.append("))");
		}
		sql.append(" ORDER BY E.Lname, E.Fname");

		StringBuilder employees = new StringBuilder();
		try (Connection conn = getConnection(filledDatabase);
				PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
			int parameterIndex = 1;
			for (int i = 0; i < selectedDepartments.size(); i++) {
				stmt.setString(parameterIndex++, selectedDepartments.get(i));
			}
			for (String selectedProject : selectedProjects) {
				stmt.setString(parameterIndex++, selectedProject);
			}
			try (ResultSet rs = stmt.executeQuery()) {
				while (rs.next()) {
					employees.append(rs.getString("Fname"))
							.append(' ')
							.append(rs.getString("Lname"))
							.append('\n');
				}
			}
			textAreaEmployee.setText(employees.toString());
		} catch (SQLException ex) {
			JOptionPane.showMessageDialog(this, ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
		}
	}

	/**
	 * Create the frame.
	 */
	public EmployeeSearchFrame() {
		setTitle("Employee Search");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 450, 347);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		
		setContentPane(contentPane);
		contentPane.setLayout(null);
		
		JLabel lblNewLabel = new JLabel("Database:");
		lblNewLabel.setFont(new Font("Times New Roman", Font.BOLD, 12));
		lblNewLabel.setBounds(21, 23, 59, 14);
		contentPane.add(lblNewLabel);
		
		txtDatabase = new JTextField();
		txtDatabase.setBounds(90, 20, 193, 20);
		contentPane.add(txtDatabase);
		txtDatabase.setColumns(10);
		
		JButton btnDBFill = new JButton("Fill");
		btnDBFill.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				fillLists();
			}
		});
		
		btnDBFill.setFont(new Font("Times New Roman", Font.BOLD, 12));
		btnDBFill.setBounds(307, 19, 68, 23);
		contentPane.add(btnDBFill);
		
		JLabel lblDepartment = new JLabel("Department");
		lblDepartment.setFont(new Font("Times New Roman", Font.BOLD, 12));
		lblDepartment.setBounds(52, 63, 89, 14);
		contentPane.add(lblDepartment);
		
		JLabel lblProject = new JLabel("Project");
		lblProject.setFont(new Font("Times New Roman", Font.BOLD, 12));
		lblProject.setBounds(255, 63, 47, 14);
		contentPane.add(lblProject);
		
		lstProject = new JList<String>(project);
		lstProject.setFont(new Font("Tahoma", Font.PLAIN, 12));
		lstProject.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		JScrollPane scrollProject = new JScrollPane(lstProject);
		scrollProject.setBounds(225, 84, 150, 42);
		contentPane.add(scrollProject);
		
		chckbxNotDept = new JCheckBox("Not");
		chckbxNotDept.setBounds(71, 133, 59, 23);
		contentPane.add(chckbxNotDept);
		
		chckbxNotProject = new JCheckBox("Not");
		chckbxNotProject.setBounds(270, 133, 59, 23);
		contentPane.add(chckbxNotProject);
		
		lstDepartment = new JList<String>(department);
		lstDepartment.setFont(new Font("Tahoma", Font.PLAIN, 12));
		lstDepartment.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		JScrollPane scrollDepartment = new JScrollPane(lstDepartment);
		scrollDepartment.setBounds(36, 84, 172, 40);
		contentPane.add(scrollDepartment);
		
		JLabel lblEmployee = new JLabel("Employee");
		lblEmployee.setFont(new Font("Times New Roman", Font.BOLD, 12));
		lblEmployee.setBounds(52, 179, 89, 14);
		contentPane.add(lblEmployee);
		
		JButton btnSearch = new JButton("Search");
		btnSearch.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				searchEmployees();
			}
		});
		btnSearch.setBounds(80, 276, 89, 23);
		contentPane.add(btnSearch);
		
		JButton btnClear = new JButton("Clear");
		btnClear.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				textAreaEmployee.setText("");
				lstDepartment.clearSelection();
				lstProject.clearSelection();
				chckbxNotDept.setSelected(false);
				chckbxNotProject.setSelected(false);
			}
		});
		btnClear.setBounds(236, 276, 89, 23);
		contentPane.add(btnClear);
		
		textAreaEmployee = new JTextArea();
		textAreaEmployee.setEditable(false);
		JScrollPane scrollEmployee = new JScrollPane(textAreaEmployee);
		scrollEmployee.setBounds(36, 197, 339, 68);
		contentPane.add(scrollEmployee);
	}
}
