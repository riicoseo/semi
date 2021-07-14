package dao;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;

import boardconfig.BoardConfig;
import dto.Board2DTO;

public class Board2DAO {
	
	private Board2DAO() {}
	private static Board2DAO instance;
	public synchronized static Board2DAO getInstance() {
		if(instance ==null) {
			instance = new Board2DAO();
		}
		return instance;
	}
	
	private Connection getConnection() throws Exception {
		Context ctx = new InitialContext();
		DataSource ds = (DataSource)ctx.lookup("java:comp/env/jdbc/oracle");
		return ds.getConnection();
	}
	
	
	
//========= 게시판 기본 메서드 ======================================================================	
	public List<Board2DTO> selectAll() throws Exception {
		String sql ="select * from board2";
		List<Board2DTO> list = new ArrayList<>();
		try(Connection con = this.getConnection(); 
			PreparedStatement pstat = con.prepareStatement(sql);
			ResultSet rs = pstat.executeQuery();){
			while(rs.next()) {
				int board_seq2 = rs.getInt("board_seq2");
				String id2 = rs.getNString("id2");
				String title2 = this.ReXSSFilter(rs.getNString("title2"));
				String content2 =rs.getNString("content2");
				Date write_date2 = rs.getDate("write_date2");
				int view_count2 = rs.getInt("view_count2");
				String notice2 = rs.getNString("notice2");
				list.add(new Board2DTO(board_seq2,id2,title2,content2,write_date2,view_count2,notice2));
			}
			return list;
		}
	}
	
	
	public Board2DTO detail(int board_seq) throws Exception {
		String sql ="select * from board2 where board_seq=?";
		Board2DTO dto = new Board2DTO();
		try(Connection con = this.getConnection();
				PreparedStatement pstat =con.prepareStatement(sql)){
			pstat.setInt(1, board_seq);
			try(ResultSet rs =pstat.executeQuery();){
				if(rs.next()) {
					dto.setBoard_seq2(rs.getInt("board_seq2"));
					dto.setId2(rs.getNString("id2"));
					dto.setTitle2(this.ReXSSFilter(rs.getString("title2")));
					dto.setContent2(rs.getString("content2"));
					dto.setWrite_date2(rs.getDate("write_date2"));
					dto.setView_count2(rs.getInt("view_count2"));
					dto.setNotice2(rs.getNString("notice2"));
				}
				return dto;
			}
		}	
	}
	
	public String XSSFilter(String target) {
		if(target!=null){
			target = target.replaceAll("<","&lt;");	
			target = target.replaceAll(">","&gt;");		
			target = target.replaceAll("&","&amp;");		
		}
		return target;
	}
	
	
	 //XSSFilter 역으로 다시 해서 화면에 뿌리기
	public String ReXSSFilter(String target) {
		if(target!=null){
			target = target.replaceAll("&lt;","<");	
			target = target.replaceAll("&gt;",">");		
			target = target.replaceAll("&amp;","&");		
		}
		return target;
	}
	
	
	public int insert(Board2DTO dto) throws Exception {
		String sql = "insert into board2 values(?,?,?,?,sysdate,0,?)";
		try (Connection con = this.getConnection(); PreparedStatement pstat = con.prepareStatement(sql);) {
			pstat.setInt(1, dto.getBoard_seq2());
			pstat.setString(2, dto.getId2());
			pstat.setString(3, dto.getTitle2());
			pstat.setString(4, dto.getContent2());
			pstat.setString(5, dto.getNotice2());
			int result = pstat.executeUpdate();
			con.commit();
			return result;
		}
	}
	
	public List<Board2DTO> search(String category, String searchWord) throws Exception {
		String sql ="select * from board2 where "+category +" like ?";
		List<Board2DTO> list = new ArrayList<Board2DTO>();		
		try(Connection con = this.getConnection(); 
					PreparedStatement pstat = con.prepareStatement(sql);){
					pstat.setString(1, "%"+searchWord+"%");
					try(ResultSet rs = pstat.executeQuery();){
						while(rs.next()) {
						  int board_seq2 = rs.getInt("board_seq2");
						  String id2 = rs.getNString("id2");
						  String title2 = this.ReXSSFilter(rs.getString("title2"));
						  String content2 = rs.getString("content2");
						  Date write_date2 = rs.getDate("write_date2");
					      int view_count2 = rs.getInt("view_count2");
					      String notice2 = rs.getNString("notice2");
							list.add(new Board2DTO(board_seq2,id2, title2, content2,write_date2, view_count2,notice2));
						}
					}return list;
				}
	}
	
	
	
	
	
	
	
//========= 게시판  페이징 처리 ======================================================================
	
	private int getRecordCount() throws Exception {
		String sql = "select count(*) from board2";
		try (Connection con = this.getConnection();
			 PreparedStatement pstat = con.prepareStatement(sql);
			 ResultSet rs = pstat.executeQuery();) {
			rs.next();
			return rs.getInt(1);

		}
	}
	// 오버 로딩해서 다시 하나 더 만들기
		private int getRecordCount(String category, String keyword) throws Exception {
			String sql = "select count(*) from board2 where " + category + " like ?";
			try (Connection con = this.getConnection(); PreparedStatement pstat = con.prepareStatement(sql);) {
				pstat.setString(1, "%" + keyword + "%");
				try (ResultSet rs = pstat.executeQuery();) {
					rs.next();
					return rs.getInt(1);
				}
			}
		}
		
		
	public List<String> getPageNavi(int currentPage, String category, String searchWord) throws Exception {
		
		int recordTotalCount ;
		
		if(searchWord==null||searchWord.contentEquals("")) {
			recordTotalCount=this.getRecordCount();
		}else {
			recordTotalCount=this.getRecordCount(category,searchWord);
		}
		
		int recordCountPerPage = BoardConfig.RECORD_COUNT_PER_PAGE; // 한 페이지 당 보여줄 게시글의 개수
		int naviCountPerPage = BoardConfig.NAVI_COUNT_PER_PAGE; // 내 위치 페이지를 기준으로 시작부터 끝까지의 페이지가 총 몇개인지

		int pageTotalCount = 0;   
		// 전체 레코드를 페이지당 보여줄 게시글 수 로 나눠서, 나머지가 0보다 크다면 1페이지를 더 추가해줘라!
		if (recordTotalCount % recordCountPerPage > 0) {
			pageTotalCount = (recordTotalCount / recordCountPerPage) + 1;
		} else {
			// 전체 레코드를 페이지당 보여줄 게시글 수 로 나눠서, 나머지가 0이면
			// 페이지의 게시글 수와 레코드 개수가 딱 맞아 떨어지니까, 총 만들어야 할 전체 페이지 개수도 딱 맞아 떨어진다!
			pageTotalCount = recordTotalCount / recordCountPerPage;
		}

		
		if (currentPage > pageTotalCount) {
			currentPage = pageTotalCount;
		} else if (currentPage < 1) {
			currentPage = 1;
		}

		// 페이지 네비게이터의 첫번째 시작 숫자를 알 수 있는 코드
		int startNavi = (currentPage - 1) / naviCountPerPage * naviCountPerPage + 1;
		// 페이지 네비게이터의 마지막 숫자를 알 수 있는 코드
		int endNavi = startNavi + (naviCountPerPage - 1);
		if (endNavi > pageTotalCount) {
			endNavi = pageTotalCount;
		}

		// 페이지 < 1 2 3 4 5> 처럼 이전, 이후 표시 만드는 코드
		boolean needPrev = true;
		boolean needNext = true;

		if (startNavi == 1) {
			needPrev = false;
		}
		if (endNavi == pageTotalCount) {
			needNext = false;
		}

		List<String> pageNavi = new ArrayList<>();

		if (needPrev) {
			pageNavi.add("<");
		}

		for (int i = startNavi; i <= endNavi; i++) {
			pageNavi.add(String.valueOf(i)); // 숫자 i를 string으로 변환해서 add 해주기!
		}
		if (needNext) {
			pageNavi.add(">");
		}

		return pageNavi;

	}
	
	public List<Board2DTO> getPageList(int startNum, int endNum) throws Exception {
		String sql = "select * from " + "(select " + "row_number() over(order by notice2 desc, board_seq2 desc) rnum," + "board_seq2,"+"id2," + "title2,"
				+ "content2," + "write_date2," + "view_count2, notice2 " + "from board2) " + "where " + "rnum between ? and ?";
		try (Connection con = this.getConnection(); PreparedStatement pstat = con.prepareStatement(sql);) {
			pstat.setInt(1, startNum);
			pstat.setInt(2, endNum);

			try (ResultSet rs = pstat.executeQuery();) {
				List<Board2DTO> list = new ArrayList<Board2DTO>();

				while (rs.next()) {
					int board_seq2 = rs.getInt("board_seq2");
					String id2 = rs.getNString("id2");
					String title2 = this.ReXSSFilter(rs.getString("title2"));
					String content2 = rs.getString("content2");
					Date write_date2 = rs.getDate("write_date2");
					int view_count2 = rs.getInt("view_count2");
					String notice2 = rs.getNString("notice2");
					list.add(new Board2DTO(board_seq2,id2,title2,content2,write_date2,view_count2,notice2));
				}
				return list;
			}
		}
	}
	// 검색 후, 페이지 리스트를 가져오는 메서드를 오버로딩해서 한번 더 만들기!
		public List<Board2DTO> getPageList(int startNum, int endNum, String category, String keyword) throws Exception {
			String sql = "select * from " + "(select " + "row_number() over(order by notice2 desc, board_seq2 desc) rnum," + "board_seq2,"+"id2," + "title2,"
					+ "content2," + "write_date2," + "view_count2, notice2 " + "from board2 where "+category+" like ?) " + "where " + "rnum between ? and ?";
			try (Connection con = this.getConnection(); 
				PreparedStatement pstat = con.prepareStatement(sql);) {
				
				
				pstat.setString(1, "%" + keyword + "%");
				pstat.setInt(2, startNum);
				pstat.setInt(3, endNum);

				try (ResultSet rs = pstat.executeQuery();) {
					List<Board2DTO> list = new ArrayList<Board2DTO>();

					while (rs.next()) {
						int board_seq2 = rs.getInt("board_seq2");
						String id2 = rs.getNString("id2");
						String title2 = this.ReXSSFilter(rs.getString("title2"));
						String content2 = rs.getString("content2");
						Date write_date2 = rs.getDate("write_date2");
						int view_count2 = rs.getInt("view_count2");
						String notice2 = rs.getNString("notice2");
						list.add(new Board2DTO(board_seq2, id2,title2,content2,write_date2,view_count2,notice2));
					}
					return list;
				}
			}
		}
		
//========= 게시판  페이징 처리 끝! ======================================================================
	
		
		public int getSeq() throws Exception {
			String sql ="select board_seq2.nextval from dual";
			try (Connection con = this.getConnection();
					PreparedStatement pstat = con.prepareStatement(sql);
					ResultSet rs = pstat.executeQuery();) {
				rs.next();
				return rs.getInt(1);
			}
		}
		
		// 조회수 출력 ---------------------------------------------------------------
		   public int view_count(int board_seq2) throws Exception{
		      String sql="update board2 set view_count2 = view_count2+1 where board_seq2=?";
		      try(Connection con = this.getConnection(); 
		            PreparedStatement pstat = con.prepareStatement(sql);){
		         pstat.setInt(1,board_seq2);

		         int result = pstat.executeUpdate();
		         con.commit();
		         return result;
		      }
		   }
		   // 게시글 삭제 -------------------------------------------------------------------
		   public int delete(int board_seq2) throws Exception{
		      String sql = "delete from board2 where board_seq2 = ?";
		      try(
		            Connection con = this.getConnection();
		            PreparedStatement pstat = con.prepareStatement(sql);
		            ){
		         pstat.setInt(1, board_seq2);
		   
		         int result = pstat.executeUpdate();
		         con.commit();
		         return result;
		      }
		   }
		   
		   
		   // 게시글 수정
		   public int modify(int board_seq2,String reTitle2,String reContent2,String notice2) throws Exception {
			   String sql ="update board2 set title2=?, content2=?, notice=2? where board_seq=2?";
			   try(Connection con = this.getConnection(); 
				   PreparedStatement pstat = con.prepareStatement(sql)){
				   pstat.setNString(1, reTitle2);
				   pstat.setNString(2, reContent2);
				   pstat.setNString(3, notice2);
				   pstat.setInt(4, board_seq2);
				   int result =pstat.executeUpdate();
				   con.commit();
				   return result;
			   }
		   }
		   
		   
		   
}