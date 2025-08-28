import java.sql.*;
import java.util.*;

public class DutyAllocator {

    static class Faculty {
        String staffId;
        String fullName;
        int cadreId;
        int baseWorkload;
        int assigned;

        Faculty(String staffId, String fullName, int cadreId, int baseWorkload) {
            this.staffId = staffId;
            this.fullName = fullName;
            this.cadreId = cadreId;
            this.baseWorkload = baseWorkload;
            this.assigned = 0;
        }
    }

    public static List<Faculty> AssignWorkload(int TOTAL_DUTIES, String username, String password,String dbname) {
        List<Faculty> faculties = new ArrayList<>();
        Set<String> excluded = new HashSet<>(Arrays.asList(
                "Dr. Anitha H",
                "Dr. Renuka A",
                "Dr. B.Kishore",
                "Dr. Roopalakshmi R",
                "Dr. T. Sujithra"
        ));

        int totalBase = 0;
        Connection conn = null;

        try {
            conn = DriverManager.getConnection(
                    "jdbc:postgresql://localhost:5432/"+dbname, username, password);

            String fetchQuery = """
                SELECT s.staff_id,
                       CONCAT(s.first_name, ' ', s.last_name) AS full_name,
                       ar.cadre_id,
                       ar.examduty_base_workload
                FROM staff s
                JOIN academic_rank ar ON s.academic_rank_id = ar.academic_rank_id
                WHERE s.staff_type = 'TEACHING'
                ORDER BY ar.cadre_id ASC, s.staff_id ASC
                """;

            PreparedStatement fetchStmt = conn.prepareStatement(fetchQuery);
            ResultSet rs = fetchStmt.executeQuery();

            while (rs.next()) {
                String staffId = rs.getString("staff_id");
                String fullName = rs.getString("full_name");
                int cadreId = rs.getInt("cadre_id");
                int baseWorkload = rs.getInt("examduty_base_workload");

                faculties.add(new Faculty(staffId, fullName, cadreId, baseWorkload));
            }

            rs.close();
            fetchStmt.close();

            for (Faculty f : faculties) {
                if (excluded.contains(f.fullName)) {
                    f.assigned = 0;
                } else {
                    f.assigned = f.baseWorkload;
                    totalBase += f.assigned;
                }
            }

            int remaining = TOTAL_DUTIES - totalBase;

            
            List<Faculty> cadre5tocadre2 = new ArrayList<>();
            for (Faculty f : faculties) {
                if (!excluded.contains(f.fullName) && f.cadreId >= 2 && f.cadreId <= 5) {
                    cadre5tocadre2.add(f);
                }
            }

           
            cadre5tocadre2.sort((a, b) -> Integer.compare(b.cadreId, a.cadreId)); 

           
            while (remaining > 0) {
                for (Faculty f : cadre5tocadre2) {
                    if (remaining == 0) break;
                    f.assigned += 1;
                    remaining--;
                }
            }

            
            String updateQuery = "UPDATE staff SET examduty_workload = ? WHERE staff_id = ?";
            PreparedStatement updateStmt = conn.prepareStatement(updateQuery);

            for (Faculty f : faculties) {
                updateStmt.setInt(1, f.assigned);
                updateStmt.setString(2, f.staffId);
                updateStmt.executeUpdate();
            }

            updateStmt.close();

        } catch (SQLException e) {
            System.err.println("Database Error: " + e.getMessage());
        } finally {
            try {
                if (conn != null && !conn.isClosed()) conn.close();
            } catch (SQLException ex) {
                System.err.println("Failed to close connection: " + ex.getMessage());
            }
        }

        return faculties;
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
    
        System.out.print("Enter total number of duties: ");
        int TOTAL_DUTIES = scanner.nextInt();

        System.out.print("Enter database name: ");
        String dbname = scanner.nextLine();
    
        System.out.print("Enter DB username: ");
        String username = scanner.nextLine();
        
        System.out.print("Enter DB password: ");
        String password = scanner.nextLine();
        
        List<Faculty> faculties = AssignWorkload(TOTAL_DUTIES, username, password,dbname);
        
        System.out.printf("%-10s | %-30s | Cadre | Base | Assigned\n", "Staff ID", "Name");
        System.out.println("-----------------------------------------------------------------------");
        for (Faculty f : faculties) {
            System.out.printf("%-10s | %-30s |  %3d  |  %3d  |   %3d\n",
                    f.staffId, f.fullName, f.cadreId, f.baseWorkload, f.assigned);
        }

        scanner.close();}
}
