package com.example.mycalendar

import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import android.webkit.WebView

class PrivacyActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_privacy)

        // 🔙 뒤로가기 버튼 눌렀을 때 현재 화면 닫기
        val backButton = findViewById<ImageButton>(R.id.btn_back)
        backButton.setOnClickListener {
            finish()
        }

        // 📜 약관 본문 내용 설정
        val webView = findViewById<WebView>(R.id.privacyWebView)
        val termsHtml = """
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-size: 14px; line-height: 1.6; padding: 16px; }
                    h2 { font-size: 16px; font-weight: bold; }
                    table {
                        width: 100%;
                        border-collapse: collapse;
                        margin: 12px 0;
                    }
                    th, td {
                        border: 1px solid #aaa;
                        padding: 8px;
                        text-align: left;
                    }
                    ul { padding-left: 20px; }
                    li { margin-bottom: 6px; }
                    .note { font-size: 13px; color: #555; margin-left: 10px; }
                </style>
            </head>
            <body>
            <h2>제1조 (수집하는 개인정보 항목)</h2>
            <p>회사는 회원가입 및 서비스 제공을 위해 아래와 같은 정보를 수집합니다.</p>

            <table>
                <tr>
                    <th>항목</th>
                    <th>수집 목적</th>
                </tr>
                <tr>
                    <td>이름, 전화번호, 아이디, 비밀번호</td>
                    <td>회원 식별, 로그인 및 서비스 이용</td>
                </tr>
                <tr>
                    <td>일정 제목, 일정 날짜 (필수)</td>
                    <td>일정 생성 및 관리 기능 제공</td>
                </tr>
                <tr>
                    <td>시간, 장소, 분야, 메모 (선택)</td>
                    <td>일정 상세 입력 및 개인화된 서비스 제공</td>
                </tr>
            </table>
            
            <h2>제2조 (개인정보의 이용 목적)</h2>
            <p>회사는 수집한 개인정보를 다음의 목적을 위해 이용합니다.</p>

            <ol>
                <li>회원 가입, 본인 확인, 계정 관리</li>
                <li>일정 등록, 수정, 알림 기능 제공</li>
                <li>일정 요약 기능 제공
                    <ul>
                        <li>- 일정 요약 기능 제공을 위해 일정 내용 일부가 외부 AI 처리 서버(<b>Gemini API</b>)로 전송될 수 있습니다.</li>
                        <li>- 해당 데이터는 저장되지 않으며, 학습에도 사용되지 않습니다.</li>
                        <li>- 민감한 정보가 포함되지 않도록 이용자 주의가 필요합니다.</li>
                    </ul>
                </li>
                <li>고객 문의 응대 및 서비스 품질 개선</li>
                <li>푸시 알림 제공
                    <ul>
                        <li>- 푸시 알림에는 요약된 일정 내용(시간, 제목, 장소 등)이 포함될 수 있습니다.</li>
                    </ul>
                </li>
            </ol>

            <p><small>※ 회사는 마케팅 목적의 광고성 정보를 발송하지 않습니다.</small></p>

            <br>

            <h2>제3조 (개인정보의 수집 방법)</h2>
            <ul>
                <li>회원가입 시 이용자가 직접 입력</li>
                <li>일정 작성 또는 수정 시 이용자가 직접 입력</li>
                <li>AI 요약 기능 실행 시 일정 내용 일부가 외부 처리 서버로 전송됨</li>
            </ul>
            
            <h2>제4조 (개인정보의 보유 및 이용 기간)</h2>
            <p>회사는 수집한 개인정보를 다음 기준에 따라 보관 및 파기합니다.</p>

            <table>
                <tr>
                    <th>항목</th>
                    <th>보유 기간</th>
                    <th>관련 법령</th>
                </tr>
                <tr>
                    <td>회원가입 정보</td>
                    <td>회원 탈퇴 후 7일 유예기간 경과 시 즉시 파기</td>
                    <td>-</td>
                </tr>
                <tr>
                    <td>계약 및 청약철회 기록</td>
                    <td>5년</td>
                    <td>전자상거래법</td>
                </tr>
                <tr>
                    <td>소비자 분쟁처리 기록</td>
                    <td>3년</td>
                    <td>전자상거래법</td>
                </tr>
                <tr>
                    <td>서비스 접속 로그</td>
                    <td>1년</td>
                    <td>통신비밀보호법</td>
                </tr>
            </table>

            <br>

            <h2>제5조 (개인정보의 제3자 제공 및 위탁)</h2>
            <p>회사는 이용자의 개인정보를 제3자에게 제공하거나 외부에 위탁하지 않습니다.</p>
            <p>향후 위탁이 필요한 경우에는 사전 고지 및 동의를 거쳐 처리할 예정입니다.</p>
            
            <h2>제6조 (개인정보의 파기 절차 및 방법)</h2>
            <ul>
                <li>전자적 파일: 복구할 수 없는 방식으로 영구 삭제</li>
                <li>인쇄물: 분쇄 또는 소각</li>
                <li>회원 탈퇴 또는 수집 목적 달성 시 지체 없이 파기</li>
            </ul>

            <br>

            <h2>제7조 (이용자의 권리와 행사 방법)</h2>
            <ul>
                <li>회원은 개인정보 열람, 정정, 삭제, 처리정지 등을 요청할 수 있습니다.</li>
                <li>회원 탈퇴 및 개인정보 동의 철회는 [내 정보 &gt; 계정 삭제]를 통해 직접 가능합니다.</li>
            </ul>

            <br>

            <h2>제8조 (개인정보 보호를 위한 조치)</h2>
            <ul>
                <li>비밀번호는 복호화 불가능한 암호화 방식으로 저장됩니다.</li>
                <li>개인정보 접근 권한을 최소화하고 정기적 보안 교육을 시행합니다.</li>
                <li>외부로부터의 무단 접근을 방지하기 위한 시스템을 운영합니다.</li>
            </ul>
            
            <h2>제9조 (개인정보 보호 책임자)</h2>
            <ul>
              <li>책임자 성명: [홍길동]</li>
              <li>직위: [예: 개인정보보호책임자]</li>
              <li>이메일: [contact@yourapp.com]</li>
            </ul>

            <h2>제10조 (정책 변경 및 고지)</h2>
            <p>
              본 방침은 관련 법령 또는 회사 내부 정책 변경에 따라 개정될 수 있으며, 개정 시 최소 7일 전 서비스 내 공지사항을 통해 사전 고지합니다.
            </p>
            <ul>
              <li>최초 시행일: 2025년 7월 30일</li>
              <li>최종 개정일: 2025년 7월 30일</li>
            </ul>
        
            </body>
            </html>
""".trimIndent()

        webView.loadDataWithBaseURL(null, termsHtml, "text/html", "UTF-8", null)
    }
}