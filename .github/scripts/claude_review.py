import os
import json
import urllib.request
import subprocess
import sys

diff = subprocess.check_output(
    ["git", "diff", os.environ["BASE_SHA"], os.environ["HEAD_SHA"], "--", "client/"],
    text=True,
)

if not diff.strip():
    print("No diff found in client/, skipping review.")
    sys.exit(0)

MAX_DIFF = 80_000
truncated = len(diff) > MAX_DIFF
if truncated:
    diff = diff[:MAX_DIFF]

pr_title = os.environ.get("PR_TITLE", "")
pr_body = os.environ.get("PR_BODY", "") or "(설명 없음)"
truncated_note = "\n**⚠️ diff가 너무 커서 일부만 포함되었습니다.**" if truncated else ""

prompt = (
    "아래 PR의 코드 변경사항을 리뷰해주세요.\n\n"
    f"**PR 제목:** {pr_title}\n"
    f"**PR 설명:** {pr_body}\n"
    f"{truncated_note}\n\n"
    "```diff\n"
    f"{diff}\n"
    "```\n\n"
    "다음 항목을 중심으로 리뷰해주세요:\n"
    "- 버그 및 잠재적 오류\n"
    "- 성능 이슈\n"
    "- 코드 품질 및 가독성\n"
    "- 보안 취약점\n"
    "- 개선 제안\n\n"
    "마크다운 형식으로 작성하고, 특이사항이 없는 항목은 생략해도 됩니다."
)

payload = {
    "model": "claude-sonnet-4-6",
    "max_tokens": 4096,
    "messages": [{"role": "user", "content": prompt}],
}

req = urllib.request.Request(
    "https://api.anthropic.com/v1/messages",
    data=json.dumps(payload).encode(),
    headers={
        "x-api-key": os.environ["ANTHROPIC_API_KEY"],
        "anthropic-version": "2023-06-01",
        "content-type": "application/json",
    },
)

with urllib.request.urlopen(req) as resp:
    result = json.loads(resp.read())

review = result["content"][0]["text"]
comment = "## 🤖 Claude Code Review\n\n" + review

comment_req = urllib.request.Request(
    f"https://api.github.com/repos/{os.environ['REPO']}/issues/{os.environ['PR_NUMBER']}/comments",
    data=json.dumps({"body": comment}).encode(),
    headers={
        "Authorization": f"Bearer {os.environ['GH_TOKEN']}",
        "Accept": "application/vnd.github+json",
        "Content-Type": "application/json",
        "X-GitHub-Api-Version": "2022-11-28",
    },
)

with urllib.request.urlopen(comment_req) as resp:
    url = json.loads(resp.read())["html_url"]
    print(f"Review posted: {url}")
