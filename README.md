# 🚀 DevOps Environment Setup

Walk through of the process of setting up a robust infrastructure on AWS using EKS, DevOps best practices, and security measures. This project aims to provide necessary commands and resources to get hands-on experience in deploying, securing, and monitoring a scalable application environment and infra. (Note: All commands are for Linux:Ubuntu)

## Article

- Tools Installation
  - Iac
    - Terraform
    - OpenTofu
    - Ansible
  - AWS CLI
  - Kubernetes Stack
  - Helm
- Setting Up Infrastructure
  - EKS Cluster
  - Kubeadm Cluster
  - Minikube Cluster
- CI
  - Java, Jenkins,
  - Docker
- CD
  - ArgoCD
- Security Tools
  - SonarQube
  - Nexus
  - Trivy
- Monitoring & Logging
  - Prometheus & Grafana
  - ELK/EFK
- Service Mesh
  - Istio
- Deployment Strategries
  - Recreate
  - Rolling Update
  - Blue Green
  - Canary
- Chaos Engineering
  - Litmus
---
# 🛠️ Tools Installation
### Iac Tools
Terraform 
```bash
#install terraform
wget -O- https://apt.releases.hashicorp.com/gpg | sudo gpg --dearmor -o /usr/share/keyrings/hashicorp-archive-keyring.gpg
echo "deb [signed-by=/usr/share/keyrings/hashicorp-archive-keyring.gpg] https://apt.releases.hashicorp.com $(lsb_release -cs) main" | sudo tee /etc/apt/sources.list.d/hashicorp.list
sudo apt update && sudo apt install terraform -y
terraform -version
```
---
### Or you can use OpenTofu 
```bash
#install opentofu
TOFU_VERSION="1.7.0" # Replace version as needed
wget https://github.com/opentofu/opentofu/releases/download/v${TOFU_VERSION}/tofu_${TOFU_VERSION}_linux_amd64.zip
sudo apt install unzip -y
unzip tofu_${TOFU_VERSION}_linux_amd64.zip
sudo mv tofu /usr/local/bin/
tofu version
```
---
### Ansible
```bash
#install ansible
sudo apt update
sudo apt install software-properties-common -y
sudo add-apt-repository --yes --update ppa:ansible/ansible
sudo apt install ansible -y
ansible --version
```
---
### ☁️ AWS CLI
```bash
#install aws cli
curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
sudo apt install unzip -y
unzip awscliv2.zip
sudo ./aws/install
```
To Configure AWS ( You might need aws user creditials with necessary permissions)
```bash
aws configure
```
---
## ☸️ Installing Kubernetes Stack
### Install `kubectl`, `eksctl`, `containerd`, and dependencies
```bash
# Kernel modules and sysctl
cat <<EOF | sudo tee /etc/modules-load.d/k8s.conf
overlay
br_netfilter
EOF

sudo modprobe overlay
sudo modprobe br_netfilter

cat <<EOF | sudo tee /etc/sysctl.d/k8s.conf
net.bridge.bridge-nf-call-iptables  = 1
net.bridge.bridge-nf-call-ip6tables = 1
net.ipv4.ip_forward                 = 1
EOF

sudo sysctl --system
```
---
### Install Kubernetes tools
```bash
sudo apt-get install -y containerd curl apt-transport-https ca-certificates
containerd config default | sudo tee /etc/containerd/config.toml
sudo systemctl restart containerd

# Install kubeadm, kubelet, kubectl
KUBE_LATEST=$(curl -sL https://dl.k8s.io/release/stable.txt | awk 'BEGIN{FS="."}{printf "%s.%s", $1, $2}')
curl -fsSL https://pkgs.k8s.io/core:/stable:/${KUBE_LATEST}/deb/Release.key | sudo gpg --dearmor -o /etc/apt/keyrings/kubernetes-apt-keyring.gpg
echo "deb [signed-by=/etc/apt/keyrings/kubernetes-apt-keyring.gpg] https://pkgs.k8s.io/core:/stable:/${KUBE_LATEST}/deb/ /" | sudo tee /etc/apt/sources.list.d/kubernetes.list
sudo apt-get update
sudo apt-get install -y kubelet kubeadm kubectl
sudo apt-mark hold kubelet kubeadm kubectl
```
---
### Helm
```bash
sudo snap install helm --classic
```
---
# Setting Up Infrastructure
## EKS Cluster
Prerequisite - We need to create an IAM user and generate the AWS Access key and Awscli, Terraform or OpenTofu must installed
```bash
aws configure #add cred copied from IAM user
```
Clone this repo in your local machine or vm
```bash
git clone https://github.com/dummy-roro/EKS-Terraform-GitHub-Actions.git 
```
Then navigate to eks folder and 
```bash
terraform init
```
```bash
terraform plan
```
```bash
terraform apply --auto-approve
```
---
## 🧩 Kubernetes Cluster Set Up With Kubeadm
ON-BOTH
------------------
```bash
ip route | grep default | awk '{ print $9 }'

PRIMARY_IP=
POD_CIDR=10.244.0.0/16
SERVICE_CIDR=10.96.0.0/1

sudo apt-get update
sudo apt-get install -y apt-transport-https ca-certificates curl

cat <<EOF | sudo tee /etc/modules-load.d/k8s.conf
overlay
br_netfilter
EOF

sudo modprobe overlay
sudo modprobe br_netfilter

cat <<EOF | sudo tee /etc/sysctl.d/k8s.conf
net.bridge.bridge-nf-call-iptables  = 1
net.bridge.bridge-nf-call-ip6tables = 1
net.ipv4.ip_forward                 = 1
EOF

sudo sysctl --system
#installing containerd
sudo apt-get install -y containerd
sudo mkdir -p /etc/containerd
containerd config default | sed 's/SystemdCgroup = false/SystemdCgroup = true/' | sudo tee /etc/containerd/config.toml
sudo systemctl restart containerd

KUBE_LATEST=$(curl -L -s https://dl.k8s.io/release/stable.txt | awk 'BEGIN { FS="." } { printf "%s.%s", $1, $2 }')
sudo mkdir -p /etc/apt/keyrings
curl -fsSL https://pkgs.k8s.io/core:/stable:/${KUBE_LATEST}/deb/Release.key | sudo gpg --dearmor -o /etc/apt/keyrings/kubernetes-apt-keyring.gpg
echo "deb [signed-by=/etc/apt/keyrings/kubernetes-apt-keyring.gpg] https://pkgs.k8s.io/core:/stable:/${KUBE_LATEST}/deb/ /" | sudo tee /etc/apt/sources.list.d/kubernetes.list
sudo apt-get update
sudo apt-get install -y kubelet kubeadm kubectl
sudo apt-mark hold kubelet kubeadm kubectl

sudo crictl config \
    --set runtime-endpoint=unix:///run/containerd/containerd.sock \
    --set image-endpoint=unix:///run/containerd/containerd.sock

cat <<EOF | sudo tee /etc/default/kubelet
KUBELET_EXTRA_ARGS='--node-ip ${PRIMARY_IP}'
EOF
```
ON-MASTER
-----------------
```bash
POD_CIDR=10.244.0.0/16
SERVICE_CIDR=10.96.0.0/16
sudo kubeadm init --pod-network-cidr $POD_CIDR --service-cidr $SERVICE_CIDR --apiserver-advertise-address $PRIMARY_IP
mkdir ~/.kube
sudo cp /etc/kubernetes/admin.conf ~/.kube/config
sudo chown $(id -u):$(id -g) ~/.kube/config
chmod 600 ~/.kube/config

kubectl get pods -n kube-system
kubectl apply -f "https://github.com/weaveworks/weave/releases/download/v2.8.1/weave-daemonset-k8s-1.11.yaml"
kubectl get pods -n kube-system
```
ON-NODE
----------------
```bash
sudo -i
paste join cmd
```
---
## 📋 Notes
- Make sure to paste `kubeadm join` output from the master into your worker node.
- Replace `<your-primary-ip>` with the result of: `ip route | grep default | awk '{ print $9 }'`
---
## Minikube Cluster Set Up
```bash
wget https://storage.googleapis.com/minikube/releases/latest/minikube-linux-amd64
chmod +x minikube-linux-amd64
sudo mv minikube-linux-amd64 /usr/local/bin/minikube
```
To Start Minikube
```bash
#minikube start --driver=<driver> --cpus=<number> --memory=<amount>
minikube start --driver=docker --cpus=2 --memory=4096
```
---

## 📦 Installing Resources 

### ☕ Java & Jenkins

```bash
#Java installation
sudo apt update -y
sudo apt install openjdk-17-jre openjdk-17-jdk -y
java --version
# Jenkins installation
curl -fsSL https://pkg.jenkins.io/debian/jenkins.io-2023.key | sudo tee /usr/share/keyrings/jenkins-keyring.asc > /dev/null
echo deb [signed-by=/usr/share/keyrings/jenkins-keyring.asc] https://pkg.jenkins.io/debian binary/ | sudo tee /etc/apt/sources.list.d/jenkins.list > /dev/null
sudo apt-get update -y
sudo apt-get install jenkins -y
```
You can use below command to get Jenkins Admin Password

```bash
systemctl status jenkins
```
Sample Jenkinsfile using shared library
```bash
@Library('jenkins-shared-library') _ // Load your shared library

pipeline {
    agent any

    tools {
        nodejs 'nodejs23'
    }

    environment {
        SCANNER_HOME = tool 'sonar-scanner'
        IMAGE_TAG = "v${env.BUILD_NUMBER}"
    }

    options {
        disableResume()
        disableConcurrentBuilds abortPrevious: true
    }

    stages {
        stage('Cleanup Workspace') {
            steps {
                cleanWs()
            }
        }

        stage('Git Checkout') {
            steps {
                checkoutRepo('https://github.com/jaiswaladi246/3-Tier-DevSecOps-Mega-Project.git', 'dev')
            }
        }

        stage('Compile') {
            parallel {
                stage('Frontend Compile') {
                    steps {
                        compileJS('client')
                    }
                }
                stage('Backend Compile') {
                    steps {
                        compileJS('api')
                    }
                }
            }
        }

        stage('GitLeaks Scan') {
            steps {
                gitleaksScan(['client', 'api'])
            }
        }

        stage('SonarQube Analysis') {
            steps {
                sonarAnalysis('NodeJS-Project', 'NodeJS-Project')
            }
        }

        stage('Quality Gate Check') {
            steps {
                qualityGateCheck('sonar-token')
            }
        }

        stage('Trivy FS Scan') {
            steps {
                trivyScan('.', 'fs-report.html')
            }
        }

        stage('Docker Build') {
            parallel {
                stage('Frontend Build') {
                    steps {
                        dockerBuild(
                            imageName: '<your docker registery>/frontend-app',
                            imageTag: env.IMAGE_TAG,
                            dockerfile: 'client/Dockerfile',
                            context: 'client'
                        )
                    }
                }
                stage('Backend Build') {
                    steps {
                        dockerBuild(
                            imageName: '<your docker registery>/backend-app',
                            imageTag: env.IMAGE_TAG,
                            dockerfile: 'api/Dockerfile',
                            context: 'api'
                        )
                    }
                }
            }
        }

        stage('Trivy Image Scan') {
            parallel {
                stage('Frontend Image Scan') {
                    steps {
                        trivyImageScan(
                            imageName: '<your docker registery>/frontend-app',
                            imageTag: env.IMAGE_TAG
                        )
                    }
                }
                stage('Backend Image Scan') {
                    steps {
                        trivyImageScan(
                            imageName: '<your docker registery>/backend-app',
                            imageTag: env.IMAGE_TAG
                        )
                    }
                }
            }
        }

        stage('Docker Push') {
            parallel {
                stage('Frontend Push') {
                    steps {
                        dockerPush(
                            imageName: '<your docker registery>/frontend-app',
                            imageTag: env.IMAGE_TAG,
                            credentials: 'docker-hub-credentials'
                        )
                    }
                }
                stage('Backend Push') {
                    steps {
                        dockerPush(
                            imageName: '<your docker registery>/backend-app',
                            imageTag: env.IMAGE_TAG,
                            credentials: 'docker-hub-credentials'
                        )
                    }
                }
            }
        }

        stage('Update Kubernetes Image Tags') {
            steps {
                changeImageTag(
                    imageTag: env.IMAGE_TAG,
                    manifestsPath: 'kubernetes',
                    gitCredentials: 'github-credentials',
                    gitUserName: 'Jenkins CI',
                    gitUserEmail: 'jenkins@example.com',
                    repoUrl: 'https://github.com/<your-gitops-repo>/e-commerce-app.git', //change with your gitops repo
                    // (optional add) branch: 'dev'
                )
            }
        }
    }
}
```
### 🐳 Installing Docker & Permissions

```bash
sudo apt install docker.io -y
```
Change Permissions to Jenkins
```bash
sudo usermod -aG docker jenkins # use when you integrate with jenkins
```
Change Permissions to Docker
```bash
sudo usermod -aG docker ubuntu
sudo systemctl restart docker
sudo chmod 777 /var/run/docker.sock #optional
```

---

## 🧪 SonarQube (Docker)

```bash
docker run -d --name sonar -p 9000:9000 sonarqube:lts-community
```
## Nexus (Docker)

```bash
docker run -d \
  --name nexus \
  -p 8081:8081 \
  -v nexus-data:/nexus-data \
  sonatype/nexus3
```
Default admin login:

Username: admin

Password: (found in /nexus-data/admin.password inside the container)
```bash
docker exec -it nexus cat /nexus-data/admin.password
```
Or Use Docker Compose (Recommended)

Create a file named docker-compose.yml:

```bash
version: '3'
services:
  nexus:
    image: sonatype/nexus3
    container_name: nexus
    ports:
      - "8081:8081"
    volumes:
      - nexus-data:/nexus-data
    restart: unless-stopped

volumes:
  nexus-data:
```
Then start it with:
```bash
docker-compose up -d
```
---




---

## 🧭 Installing Argo CD On K8s

```bash
kubectl create namespace argocd
kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml
```
Patch service to LoadBalancer
```bash
kubectl patch svc argocd-server -n argocd -p '{"spec": {"type": "LoadBalancer"}}'
```
Or Port Forward
```bash
kubectl port-forward svc/argocd-server -n argocd 9000:80 --address 0.0.0.0 > /dev/null & 
```
### 🔑 Get Argo CD Initial Admin Password

```bash
argocd admin initial-password -n argocd
```
OR
```bash
kubectl -n argocd get secret argocd-initial-admin-secret -o jsonpath="{.data.password}" | base64 -d && echo
```
To log in argocd server from terminal
```bash
argocd login <ARGOCD_SERVER>
```
Change the password using the command
```bash
argocd account update-password
```
Example app yaml
```bash
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: guestbook
  namespace: argocd
spec:
  project: default
  source:
    repoURL: https://github.com/argoproj/argocd-example-apps.git
    targetRevision: HEAD
    path: guestbook
  destination:
    server: https://kubernetes.default.svc
    namespace: guestbook
```
(Optional) If Image Is From Github GHCR Repo You Need To Create Secrect To Prevent Image Pull Error
```bash
kubectl create secret docker-registry github-container-registry \
  --docker-server=ghcr.io \
  --docker-username=YOUR_GITHUB_USERNAME \
  --docker-password=YOUR_GITHUB_TOKEN \
  --docker-email=YOUR_EMAIL \
  -n your-namespace
```
---

## 🔐 Installing Security Tools

### Install Trivy

```bash
#install trivy
sudo apt-get install -y wget gnupg lsb-release
wget -qO - https://aquasecurity.github.io/trivy-repo/deb/public.key | sudo apt-key add -
echo deb https://aquasecurity.github.io/trivy-repo/deb $(lsb_release -sc) main | sudo tee /etc/apt/sources.list.d/trivy.list
sudo apt update
sudo apt install trivy -y
```

---


## Sealed Secrect
Install kubeseal CLI (Local Machine or on Bastion Host)

🔽 Linux:
```bash
wget https://github.com/bitnami-labs/sealed-secrets/releases/latest/download/kubeseal-linux-amd64 -O kubeseal
chmod +x kubeseal
sudo mv kubeseal /usr/local/bin/
```
Install Sealed Secrets Controller using Helm
```bash
helm repo add bitnami https://charts.bitnami.com/bitnami
helm repo update
helm install sealed-secrets bitnami/sealed-secrets
```
Optional: Set the namespace
```bash
helm install sealed-secrets bitnami/sealed-secrets --namespace kube-system
```
Fetch the Public Certificate

This is required to encrypt secrets offline with kubeseal.
```bash
kubeseal --fetch-cert \
  --controller-name=sealed-secrets-controller \
  --controller-namespace=kube-system > pub-cert.pem
```
Create and Seal a Secret

🔧 Create a Kubernetes secret (On your local machine):

```bash
kubectl create secret generic mysecret \
  --from-literal=password=mypassword \
  --dry-run=client -o yaml > mysecret.yaml
```
🔐 Seal it:

```bash
kubeseal --format=yaml < mysecret.yaml > sealedsecret.yaml
```
You can now store sealedsecret.yaml in Git safely.

Apply the Sealed Secret to the Cluster
```bash
kubectl apply -f sealedsecret.yaml
```
Integration with Vault will add soon
---
# Deployment Strategries
## 1. Rolling Update (Default in Kubernetes)
📌 Step-by-step:

Define a Deployment with the new version.

Kubernetes gradually replaces old Pods with new ones.

🧾 YAML:
```bash
apiVersion: apps/v1
kind: Deployment
metadata:
  name: myapp
spec:
  replicas: 3
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxUnavailable: 1
      maxSurge: 1
  selector:
    matchLabels:
      app: myapp
  template:
    metadata:
      labels:
        app: myapp
    spec:
      containers:
        - name: myapp
          image: myapp:v2  # change image tag to update
```
---
## 2. Blue-Green Deployment
📌 Step-by-step:

Deploy two versions (e.g., v1 and v2) with different labels.

Point the Service to either version by changing the selector.

🧾 YAML:

Deployments:
```bash
# v1
apiVersion: apps/v1
kind: Deployment
metadata:
  name: myapp-v1
spec:
  replicas: 3
  selector:
    matchLabels:
      version: v1
  template:
    metadata:
      labels:
        app: myapp
        version: v1
    spec:
      containers:
        - name: myapp
          image: myapp:v1

# v2
apiVersion: apps/v1
kind: Deployment
metadata:
  name: myapp-v2
spec:
  replicas: 3
  selector:
    matchLabels:
      version: v2
  template:
    metadata:
      labels:
        app: myapp
        version: v2
    spec:
      containers:
        - name: myapp
          image: myapp:v2
```
Service:
```bash
apiVersion: v1
kind: Service
metadata:
  name: myapp-service
spec:
  selector:
    app: myapp
    version: v1  # 🔄 change to v2 to switch traffic
  ports:
    - port: 80
      targetPort: 80
```
---
## 3. Canary Deployment (with Argo Rollouts)

Requires Argo Rollouts installed in your cluster.

📌 Step-by-step:

Replace Deployment with Rollout object.

Use steps to incrementally shift traffic.

🧾 YAML:
```bash
apiVersion: argoproj.io/v1alpha1
kind: Rollout
metadata:
  name: myapp
spec:
  replicas: 3
  strategy:
    canary:
      steps:
        - setWeight: 20
        - pause: {duration: 30s}
        - setWeight: 50
        - pause: {duration: 60s}
        - setWeight: 100
  selector:
    matchLabels:
      app: myapp
  template:
    metadata:
      labels:
        app: myapp
    spec:
      containers:
        - name: myapp
          image: myapp:v2
```
Service & Analysis can be added for more advanced rollouts.
## 4. A/B Testing (using Istio or Ingress)

Requires Istio or [NGINX Ingress Controller].

📌 Step-by-step:

Create two Deployments (v1 & v2).

Route traffic by user headers or percentage.

🧾 YAML (Istio VirtualService):
```bash
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: myapp
spec:
  hosts:
    - myapp.example.com
  http:
    - match:
        - headers:
            user-type:
              exact: beta
      route:
        - destination:
            host: myapp
            subset: v2
    - route:
        - destination:
            host: myapp
            subset: v1
```
---
## 5. Shadow Deployment

Also done using Istio or custom proxy.

📌 Step-by-step:

Send a copy of traffic to a “shadow” app.

Do not return the response from shadow.

🧾 YAML (Istio):
```bash
http:
  - route:
      - destination:
          host: myapp-v1
    mirror:
      host: myapp-v2
    mirrorPercentage:
      value: 100.0
```
---
# Chaos Engineering
## Litmus
Install Litmus ChaosCenter on kubernetes Cluster

Using Helm

Add the litmus helm repository
```bash
helm repo add litmuschaos https://litmuschaos.github.io/litmus-helm/
helm repo list
```
Create the namespace
```bash
kubectl create ns litmus
```
Install Litmus ChaosCenter
```bash
helm install chaos litmuschaos/litmus --namespace=litmus --set portal.frontend.service.type=NodePort
```
---
### To Run Bash Scripts
```bash
chmod +x setup.sh
./setup.sh
```
> 📁 Make sure you're running as a user with `sudo` privileges.

---


## 📎 License

MIT – Free to use, modify, and distribute.
