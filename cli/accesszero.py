#!/usr/bin/env python3
"""
AccessZero CLI — Identity Breach Containment & Access Revocation Tool
Usage:
    python accesszero.py list
    python accesszero.py analyze --user rahul.sharma
    python accesszero.py simulate --user rahul.sharma
    python accesszero.py contain --user rahul.sharma --admin anil.admin [--emergency]
    python accesszero.py approve --operation 1 --admin priya.security
    python accesszero.py verify --user rahul.sharma
    python accesszero.py rollback --operation 1 --admin anil.admin
    python accesszero.py audit [--target rahul.sharma]
"""

import sys
import json
import urllib.request
import urllib.error
import argparse

if sys.platform.startswith('win') and hasattr(sys.stdout, 'reconfigure'):
    try:
        sys.stdout.reconfigure(encoding='utf-8')
    except Exception:
        pass

BASE_URL = "http://localhost:8080/api/v1"

class Colors:
    HEADER = '\033[95m'
    BLUE = '\033[94m'
    CYAN = '\033[96m'
    GREEN = '\033[92m'
    WARNING = '\033[93m'
    FAIL = '\033[91m'
    ENDC = '\033[0m'
    BOLD = '\033[1m'

def http_get(path):
    url = f"{BASE_URL}{path}"
    req = urllib.request.Request(url, headers={"Accept": "application/json"})
    try:
        with urllib.request.urlopen(req) as response:
            return json.loads(response.read().decode())
    except urllib.error.HTTPError as e:
        print(f"{Colors.FAIL}HTTP Error {e.code}: {e.read().decode()}{Colors.ENDC}")
        sys.exit(1)
    except Exception as e:
        print(f"{Colors.FAIL}Connection failed to {url}: {e}{Colors.ENDC}")
        sys.exit(1)

def http_post(path, body=None):
    url = f"{BASE_URL}{path}"
    data = json.dumps(body).encode('utf-8') if body else None
    req = urllib.request.Request(url, data=data, headers={"Content-Type": "application/json", "Accept": "application/json"}, method="POST")
    try:
        with urllib.request.urlopen(req) as response:
            return json.loads(response.read().decode())
    except urllib.error.HTTPError as e:
        print(f"{Colors.FAIL}HTTP Error {e.code}: {e.read().decode()}{Colors.ENDC}")
        sys.exit(1)
    except Exception as e:
        print(f"{Colors.FAIL}Connection failed to {url}: {e}{Colors.ENDC}")
        sys.exit(1)

def print_banner():
    print(f"{Colors.CYAN}{Colors.BOLD}")
    print("=================================================================")
    print("     AccessZero — Identity Breach Containment Platform CLI      ")
    print("=================================================================")
    print(f"{Colors.ENDC}")

def cmd_list(args):
    print_banner()
    identities = http_get("/identities")
    print(f"{'ID':<4} {'USERNAME':<16} {'DEPARTMENT':<14} {'STATUS':<12} {'RISK':<10} {'SESSIONS':<9} {'PATHS':<6}")
    print("-" * 75)
    for u in identities:
        risk_color = Colors.FAIL if u.get('riskLevel') == 'CRITICAL' else Colors.GREEN
        status_color = Colors.FAIL if u.get('status') == 'CONTAINED' else Colors.GREEN
        print(f"{u['id']:<4} {u['username']:<16} {u['department']:<14} {status_color}{u['status']:<12}{Colors.ENDC} {risk_color}{u.get('riskLevel','LOW'):<10}{Colors.ENDC} {u.get('activeSessionsCount',0):<9} {u.get('accessPathCount',0):<6}")

def cmd_analyze(args):
    print_banner()
    username = args.user
    print(f"[*] Calculating Access Blast Radius for identity: {Colors.BOLD}{username}{Colors.ENDC}...\n")
    data = http_get(f"/identities/username/{username}/blast-radius")
    
    sessions = data.get('activeSessionsCount', 0)
    tokens = data.get('oauthTokensCount', data.get('activeTokensCount', 0))
    groups = data.get('ldapGroupsCount', data.get('groupsCount', 0))
    sens_groups = data.get('sensitiveGroupsCount', 0)
    apps = data.get('applicationsAffectedCount', 0)
    priv_apps = data.get('privilegedApplicationsCount', 0)
    samls = data.get('samlAssignmentsCount', data.get('samlApplicationsCount', 0))
    total_paths = data.get('totalAccessPathsCount', 0)
    priv_paths = data.get('privilegedAccessPathsCount', 0)
    
    print(f"Identity:           {Colors.BOLD}{data['username']}{Colors.ENDC} (User ID: {data['userId']})")
    risk_color = Colors.FAIL if data.get('riskLevel') == 'CRITICAL' else Colors.WARNING
    print(f"Risk Score:         {risk_color}{data.get('riskScore', 100)}/100 ({data.get('riskLevel', 'HIGH')}){Colors.ENDC}")
    print(f"Active Sessions:    {sessions}")
    print(f"OAuth Tokens:       {tokens}")
    print(f"LDAP Groups:        {groups} ({sens_groups} privileged)")
    print(f"Applications:       {apps} ({priv_apps} critical/high)")
    print(f"SAML Applications:  {samls}")
    print(f"Access Paths:       {Colors.BOLD}{total_paths}{Colors.ENDC} ({priv_paths} privileged paths)")
    
    print(f"\n{Colors.BOLD}Affected Applications:{Colors.ENDC}")
    for app in data.get('affectedApplications', []):
        app_name = app.get('name', app.get('applicationName', 'App'))
        sens_color = Colors.FAIL if app.get('sensitivityLevel') == 'CRITICAL' else (Colors.WARNING if app.get('sensitivityLevel') == 'HIGH' else Colors.BLUE)
        print(f"  • {app_name:<22} [{str(app.get('type', 'OIDC')):<8}] Sensitivity: {sens_color}{str(app.get('sensitivityLevel', 'MEDIUM')):<8}{Colors.ENDC} ({app.get('accessPathCount', 1)} paths)")

def cmd_simulate(args):
    print_banner()
    username = args.user
    print(f"[*] Running Non-Destructive Containment Simulation for: {Colors.BOLD}{username}{Colors.ENDC}...\n")
    data = http_post(f"/identities/username/{username}/simulate")
    
    curr_status = data.get('accountStatusCurrent', data.get('currentStatus', 'ACTIVE'))
    prop_status = data.get('accountStatusSimulated', data.get('proposedStatus', 'CONTAINED'))
    score = data.get('disruptionScore', 100)
    level = data.get('disruptionLevel', 'HIGH')
    req_appr = data.get('requiresApproval', True)
    actions = data.get('simulatedActionSummary', data.get('actionSummary', []))
    
    print(f"Target Identity:    {data.get('username', username)}")
    print(f"Current Status:     {curr_status} -> Proposed: {Colors.FAIL}{prop_status}{Colors.ENDC}")
    print(f"Disruption Score:   {Colors.FAIL if score >= 60 else Colors.WARNING}{score}/100 ({level}){Colors.ENDC}")
    print(f"2-Person Approval:  {Colors.BOLD}{'REQUIRED (High-Impact Identity)' if req_appr else 'NOT REQUIRED'}{Colors.ENDC}")
    
    print(f"\n{Colors.BOLD}Proposed Containment Actions:{Colors.ENDC}")
    for action in actions:
        print(f"  -> {action}")

def cmd_contain(args):
    print_banner()
    username = args.user
    admin = args.admin or "anil.admin"
    override = args.emergency
    
    print(f"[!] {Colors.FAIL}{Colors.BOLD}DISPATCHING KILL SWITCH CONTAINMENT COMMAND{Colors.ENDC}")
    print(f"Target:             {username}")
    print(f"Requested By:       {admin}")
    print(f"Emergency Override: {'ENABLED' if override else 'DISABLED'}\n")
    
    payload = {
        "username": username,
        "requestedBy": admin,
        "reason": args.reason or "Suspected credential compromise (CLI Containment)",
        "emergencyOverride": override
    }
    
    res = http_post("/containment/request", payload)
    
    if res.get('requiresApproval'):
        print(f"{Colors.WARNING}[!] CONTAINMENT PENDING APPROVAL{Colors.ENDC}")
        print(f"Operation #{res['operationId']} has been created in state: {Colors.BOLD}CONTAINMENT_PENDING{Colors.ENDC}")
        print(f"Secondary security administrator must approve:")
        print(f"  python cli/accesszero.py approve --operation {res['operationId']} --admin priya.security")
    else:
        print(f"{Colors.GREEN}[OK] Containment Workflow Completed!{Colors.ENDC}")
        print(f"Operation ID:       #{res['operationId']}")
        print(f"Final Status:       {Colors.BOLD}{res['status']}{Colors.ENDC}")
        print(f"Access Paths:       {res.get('accessPathsRevoked', 0)} / {res.get('accessPathsFound', 0)} Revoked")
        print(f"Manual Actions:     {res.get('requiresManualAction', 0)}")
        
        print(f"\n{Colors.BOLD}Actions Executed:{Colors.ENDC}")
        for action in res.get('actionsExecuted', []):
            print(f"  [OK] {action}")

def cmd_approve(args):
    print_banner()
    op_id = args.operation
    admin = args.admin or "priya.security"
    
    print(f"[*] Submitting Secondary Admin Approval for Operation #{op_id} by {admin}...\n")
    payload = {
        "approvedBy": admin,
        "notes": args.notes or "CLI approval for identity breach containment"
    }
    
    res = http_post(f"/containment/{op_id}/approve", payload)
    print(f"{Colors.GREEN}[OK] Operation #{op_id} APPROVED & EXECUTED!{Colors.ENDC}")
    print(f"Target User:        {res.get('username')}")
    print(f"Final Status:       {Colors.BOLD}{res.get('status')}{Colors.ENDC}")
    print(f"Access Paths:       {res.get('accessPathsRevoked', 0)} / {res.get('accessPathsFound', 0)} Revoked")
    for action in res.get('actionsExecuted', []):
        print(f"  [OK] {action}")

def cmd_verify(args):
    print_banner()
    username = args.user
    admin = args.admin or "it.verifier"
    
    print(f"[*] Verifying Multi-Layer Zero-Access Isolation for: {username}...\n")
    data = http_get(f"/verification/username/{username}?verifiedBy={admin}")
    
    status_color = Colors.GREEN if data.get('overallStatus') == 'CONTAINED' else Colors.WARNING
    print(f"Target:             {data.get('username')}")
    print(f"Overall Status:     {status_color}{Colors.BOLD}{data.get('overallStatus')}{Colors.ENDC}")
    print(f"Access Paths:       {data.get('accessPathsRevoked', 0)} / {data.get('accessPathsFound', 0)} Revoked")
    print(f"Manual Action Items:{data.get('requiresManualActionCount', 0)}")
    
    print(f"\n{Colors.BOLD}Provider Breakdown:{Colors.ENDC}")
    for provider, res in data.get('providerResults', {}).items():
        p_color = Colors.GREEN if res.get('status') == 'CONTAINED' else Colors.WARNING
        print(f"  • {res.get('providerName', provider):<24}: {p_color}{res.get('status', 'OK'):<10}{Colors.ENDC} - {res.get('details', '')}")
    
    if data.get('remainingRisks'):
        print(f"\n{Colors.WARNING}{Colors.BOLD}Residual Application Risks & Manual Actions Required:{Colors.ENDC}")
        for r in data['remainingRisks']:
            print(f"  ⚠ {r}")

def cmd_rollback(args):
    print_banner()
    op_id = args.operation
    admin = args.admin or "anil.admin"
    
    print(f"[*] Rolling back containment Operation #{op_id} by {admin}...\n")
    res = http_post(f"/containment/{op_id}/rollback", {"rolledBackBy": admin})
    print(f"{Colors.GREEN}✓ Identity {res.get('username')} restored to ACTIVE state!{Colors.ENDC}")
    for a in res.get('actionsExecuted', []):
        print(f"  ✓ {a}")

def cmd_audit(args):
    print_banner()
    if args.target:
        events = http_get(f"/audit/targets/{args.target}")
    else:
        events = http_get("/audit/events")
        
    print(f"{'TIMESTAMP':<20} {'ACTOR':<16} {'ACTION':<22} {'TARGET':<14} {'RESULT':<12} {'CHECKSUM':<14}")
    print("-" * 102)
    for e in events[:25]:
        ts = e.get('timestamp', '')[:19].replace('T', ' ')
        chk = (e.get('checksum') or '')[:12] + "..."
        print(f"{ts:<20} {e.get('actor',''):<16} {e.get('action',''):<22} {e.get('target',''):<14} {e.get('result',''):<12} {chk:<14}")

def main():
    parser = argparse.ArgumentParser(description="AccessZero CLI — Identity Breach Containment Platform")
    subparsers = parser.add_subparsers(dest="command")

    # list
    subparsers.add_parser("list", help="List all identities in system")

    # analyze
    p_analyze = subparsers.add_parser("analyze", help="Calculate identity access blast radius")
    p_analyze.add_argument("--user", "-u", required=True, help="Target username (e.g. rahul.sharma)")

    # simulate
    p_sim = subparsers.add_parser("simulate", help="Simulate non-destructive containment")
    p_sim.add_argument("--user", "-u", required=True, help="Target username")

    # contain
    p_contain = subparsers.add_parser("contain", help="Execute or request identity containment")
    p_contain.add_argument("--user", "-u", required=True, help="Target username")
    p_contain.add_argument("--admin", "-a", default="anil.admin", help="Requesting admin")
    p_contain.add_argument("--reason", "-r", help="Containment justification")
    p_contain.add_argument("--emergency", "-e", action="store_true", help="Emergency override (bypass 2-person rule)")

    # approve
    p_app = subparsers.add_parser("approve", help="Secondary admin approval for containment")
    p_app.add_argument("--operation", "-o", type=int, required=True, help="Operation ID")
    p_app.add_argument("--admin", "-a", default="priya.security", help="Approving admin (must differ from requester)")
    p_app.add_argument("--notes", "-n", help="Approval notes")

    # verify
    p_ver = subparsers.add_parser("verify", help="Verify multi-layer zero-access post-containment")
    p_ver.add_argument("--user", "-u", required=True, help="Target username")
    p_ver.add_argument("--admin", "-a", default="it.verifier", help="Verifying admin")

    # rollback
    p_rb = subparsers.add_parser("rollback", help="Restore / recover identity to active state")
    p_rb.add_argument("--operation", "-o", type=int, required=True, help="Operation ID")
    p_rb.add_argument("--admin", "-a", default="anil.admin", help="Admin performing rollback")

    # audit
    p_aud = subparsers.add_parser("audit", help="Inspect immutable cryptographic audit trail")
    p_aud.add_argument("--target", "-t", help="Filter by target username")

    args = parser.parse_args()

    if not args.command:
        parser.print_help()
        sys.exit(1)

    cmds = {
        "list": cmd_list,
        "analyze": cmd_analyze,
        "simulate": cmd_simulate,
        "contain": cmd_contain,
        "approve": cmd_approve,
        "verify": cmd_verify,
        "rollback": cmd_rollback,
        "audit": cmd_audit
    }

    cmds[args.command](args)

if __name__ == "__main__":
    main()
