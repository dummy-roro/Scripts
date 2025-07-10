# 🚀 DevOps Environment Setup
[![LinkedIn](https://img.shields.io/badge/Connect%20with%20me%20on-LinkedIn-blue.svg)](https://www.linkedin.com/in/myat-soe-aumg/)
[![AWS](https://img.shields.io/badge/AWS-%F0%9F%9B%A1-orange)](https://aws.amazon.com)
[![Terraform](https://img.shields.io/badge/Terraform-%E2%9C%A8-lightgrey)](https://www.terraform.io)
[![Docker](https://img.shields.io/badge/Docker-%231572B6?logo=docker&logoColor=white)](https://www.docker.com)
[![Kubernetes](https://img.shields.io/badge/Kubernetes-326CE5?logo=kubernetes&logoColor=white)](https://kubernetes.io)
[![EKS](https://img.shields.io/badge/EKS-Amazon%20Elastic%20Kubernetes%20Service-FF9900?logo=amazon-eks&logoColor=white)](https://aws.amazon.com/eks/)
[![ELK](https://img.shields.io/badge/ELK-Stack-blueviolet)](https://www.elastic.co/what-is/elk-stack)
[![EFK](https://img.shields.io/badge/EFK-Stack-orange)](https://www.elastic.co/what-is/efk-stack)
[![Jaeger](https://img.shields.io/badge/Jaeger-Tracing-ff69b4)](https://www.jaegertracing.io/)
[![Chaos Mesh](https://img.shields.io/badge/ChaosMesh-%23C9274C?style=for-the-badge&logo=Kubernetes&logoColor=white)](https://chaos-mesh.org)
[![Litmus Chaos](https://img.shields.io/badge/LitmusChaos-%230073B5?style=for-the-badge&logo=Apache%20Chaos&logoColor=white)](https://litmuschaos.io)

Walk through of the process of setting up a robust infrastructure on AWS using EKS, DevOps best practices, and security measures. This project aims to provide necessary commands and resources to get hands-on experience in deploying, securing, and monitoring a scalable application environment and infra. (Note: All commands are for Linux:Ubuntu)

## Article

- [Tools Installation](#Tools-Installation)
  - [Iac](#Iac-Tools)
    - [Terraform](#Terraform)
    - [OpenTofu](#or-you-can-use-opentofu)
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
- Security Tools
  - SonarQube
  - Nexus
  - Trivy
- CD
  - ArgoCD
- Secrect Management
  - Sealed Secret
  - Vault
- Monitoring & Observability
  - Prometheus & Grafana
- Logging
  - ELK/EFK
- Tracing
  - Jaeger
- Service Mesh
  - Istio
- Deployment Strategries
  - Recreate
  - Rolling Update
  - Blue Green
  - Canary
- Chaos Engineering
  - Litmus
  - Chaos Mesh
---
# Tools Installation
## Iac Tools
### Terraform 
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
EKS Cluster using eksctl
```bash
#create cluster
eksctl create cluster --name=democluster \
                      --region=us-east-1 \
                      --zones=us-east-1a,us-east-1b \
                      --without-nodegroup
```
```bash
#create oidc provider
eksctl utils associate-iam-oidc-provider \
    --region us-east-1 \
    --cluster democluster \
    --approve
```
```bash
#create node group
eksctl create nodegroup --cluster=democluster \
                        --region=us-east-1 \
                        --name=observability-ng-private \
                        --node-type=t3.medium \
                        --nodes-min=2 \
                        --nodes-max=3 \
                        --node-volume-size=50 \
                        --managed \
                        --asg-access \
                        --external-dns-access \
                        --full-ecr-access \
                        --appmesh-access \
                        --alb-ingress-access \
                        --node-private-networking
```
To access cluster
```bash
# Update ./kube/config file
aws eks update-kubeconfig --name observability
```
---
EKS Cluster using Terraform

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
#!/bin/bash
set -e

echo "Step 1: Install kubectl, kubeadm, and kubelet v1.33.0"

# Prepare keyrings
sudo mkdir -p /etc/apt/keyrings
sudo apt-get install -y apt-transport-https ca-certificates curl gpg

# Kubernetes repo
curl -fsSL https://pkgs.k8s.io/core:/stable:/v1.33/deb/Release.key | sudo gpg --dearmor -o /etc/apt/keyrings/kubernetes-apt-keyring.gpg
echo 'deb [signed-by=/etc/apt/keyrings/kubernetes-apt-keyring.gpg] https://pkgs.k8s.io/core:/stable:/v1.33/deb/ /' | sudo tee /etc/apt/sources.list.d/kubernetes.list

# Update and install Kubernetes components
sudo apt-get update -y
sudo apt-get install -y kubelet=1.33.0-1.1 kubeadm=1.33.0-1.1 kubectl=1.33.0-1.1 vim git curl wget
sudo apt-mark hold kubelet kubeadm kubectl

echo "Step 2: Swap Off and Kernel Modules Setup"
sudo sed -i '/ swap / s/^\(.*\)$/#\1/g' /etc/fstab
sudo swapoff -a
sudo modprobe overlay
sudo modprobe br_netfilter

# Persist kernel modules
cat <<EOF | sudo tee /etc/modules-load.d/containerd.conf
overlay
br_netfilter
EOF

# Kernel parameters for Kubernetes networking
cat <<EOF | sudo tee /etc/sysctl.d/kubernetes.conf
net.bridge.bridge-nf-call-ip6tables = 1
net.bridge.bridge-nf-call-iptables = 1
net.ipv4.ip_forward = 1
EOF

# Apply sysctl params
sudo sysctl --system


echo "Step 3: Install and Configure Containerd"

# Check if containerd is already installed
if ! command -v containerd &> /dev/null
then
    echo "Containerd not found, installing..."

    # Add Docker repo key and repository
    sudo mkdir -p /etc/apt/keyrings
    curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker-archive-keyring.gpg

    echo \
    "deb [arch=amd64 signed-by=/etc/apt/keyrings/docker-archive-keyring.gpg] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" \
    | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

    sudo apt-get update -y

    sudo DEBIAN_FRONTEND=noninteractive apt-get install -y \
      -o Dpkg::Options::="--force-confold" \
      --allow-downgrades --allow-change-held-packages containerd.io
else
    echo "Containerd is already installed, skipping installation."
fi

# Always configure containerd
sudo mkdir -p /etc/containerd
containerd config default | sudo tee /etc/containerd/config.toml > /dev/null

# Modify config.toml to use SystemdCgroup
sudo sed -i 's/SystemdCgroup = false/SystemdCgroup = true/' /etc/containerd/config.toml

sudo systemctl restart containerd
sudo systemctl enable containerd


# Enable kubelet
sudo systemctl enable kubelet

echo "Step 4: Pull Kubernetes images and init cluster"

# Pull Kubernetes images
sudo kubeadm config images pull --cri-socket unix:///run/containerd/containerd.sock --kubernetes-version v1.33.0

# Initialize cluster
sudo kubeadm init \
  --pod-network-cidr=10.244.0.0/16 \
  --upload-certs \
  --kubernetes-version=v1.33.0 \
  --control-plane-endpoint="$(hostname)" \
  --ignore-preflight-errors=all \
  --cri-socket unix:///run/containerd/containerd.sock

# Setup kubeconfig for user
mkdir -p $HOME/.kube
sudo cp -i /etc/kubernetes/admin.conf $HOME/.kube/config
sudo chown $(id -u):$(id -g) $HOME/.kube/config
export KUBECONFIG=$HOME/.kube/config

echo "Step 5: Install CNI Plugin"

echo "[*] Installing Calico CNI..."
kubectl apply -f https://raw.githubusercontent.com/projectcalico/calico/v3.27.0/manifests/calico.yaml

# Optional: Install Cilium instead of Calico (commented out by default)
# echo "[*] Installing Cilium CLI..."
# curl -L --remote-name https://github.com/cilium/cilium-cli/releases/latest/download/cilium-linux-amd64.tar.gz
# sudo tar xzvf cilium-linux-amd64.tar.gz -C /usr/local/bin
# rm cilium-linux-amd64.tar.gz
#
# echo "[*] Installing Cilium CNI..."
# cilium install --version 1.15.0

# Remove control-plane taint so pods can be scheduled on master
kubectl taint nodes $(hostname) node-role.kubernetes.io/control-plane:NoSchedule- || true

echo "Kubernetes cluster setup is complete!"
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

Jenkinsfile for Terraform Actions to create EKS cluster using shared library

```bash
@Library('your-shared-lib') _

pipeline {
    agent any
    parameters {
        choice(name: 'terraformAction', choices: ['plan', 'apply', 'destroy'], description: 'Terraform Action')
        string(name: 'env', defaultValue: 'dev', description: 'Environment')
    }
    stages {
        stage('Init') {
            steps {
                script {
                    init([
                        awsCreds: 'aws-creds',
                        awsRegion: 'us-east-1',
                        terraformDir: 'eks',
                        env: params.env
                    ])
                }
            }
        }
        stage('Validate') {
            steps {
                script {
                    validate([
                        awsCreds: 'aws-creds',
                        awsRegion: 'us-east-1',
                        terraformDir: 'eks',
                        env: params.env
                    ])
                }
            }
        }
        stage('Terraform Action') {
            steps {
                script {
                    def config = [
                        awsCreds: 'aws-creds',
                        awsRegion: 'us-east-1',
                        terraformDir: 'eks',
                        env: params.env
                    ]

                    if (params.terraformAction == 'plan') {
                        plan(config)
                    } else if (params.terraformAction == 'apply') {
                        apply(config)
                    } else if (params.terraformAction == 'destroy') {
                        destroy(config)
                    } else {
                        error "Invalid action ${params.terraformAction}"
                    }
                }
            }
        }
    }
}
```

Jenkinsfile for CI pipeline using shared library

```bash
@Library('jenkins-shared-library') _ // Load your shared library

def deployApproved = false // Declare at top level for pipeline-wide scope

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
                checkoutRepo('https://github.com/<your-repo>.git', 'dev') //change with your repo and branch
            }
        }

        stage('Compile Code') {
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

        stage('SCA-Dependency Scanning') {
            parallel {
                stage('NPM Audit') {
                    steps {
                        npmAudit(['client', 'api'])
                    }
                }
                stage('OWASP Scan') {
                    steps {
                        owaspDependencyCheck(['client', 'api'])
                    }
                }
            }
        }

        stage('SAST-SonarQube Analysis') {
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
                            imageName: '<your docker registry>/frontend-app', //replace with your docker registry
                            imageTag: env.IMAGE_TAG,
                            dockerfile: 'client/Dockerfile',
                            context: 'client'
                        )
                    }
                }
                stage('Backend Build') {
                    steps {
                        dockerBuild(
                            imageName: '<your docker registry>/backend-app',
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
                            imageName: '<your docker registry>/frontend-app',
                            imageTag: env.IMAGE_TAG
                        )
                    }
                }
                stage('Backend Image Scan') {
                    steps {
                        trivyImageScan(
                            imageName: '<your docker registry>/backend-app',
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
                            imageName: '<your docker registry>/frontend-app',
                            imageTag: env.IMAGE_TAG,
                            credentials: 'docker-hub-credentials'
                        )
                    }
                }
                stage('Backend Push') {
                    steps {
                        dockerPush(
                            imageName: '<your docker registry>/backend-app',
                            imageTag: env.IMAGE_TAG,
                            credentials: 'docker-hub-credentials'
                        )
                    }
                }
            }
        }

        stage('Deploy - AWS EC2') {
            when {
                expression { env.BRANCH_NAME?.startsWith('dev/') }
            }
            steps {
                sshagent(credentials: ['aws-dev-deploy-ec2-instance']) {
                    sh """
                        # Copy docker-compose.yml to EC2
                        scp -o StrictHostKeyChecking=no docker-compose.yml ubuntu@<your-ec2-ip-address>:/home/ubuntu/
        
                        # SSH into EC2 and deploy
                        ssh -o StrictHostKeyChecking=no ubuntu@<your-ec2-ip-address> << EOF
                            cd /home/ubuntu
        
                            # Pull updated images
                            docker-compose pull
        
                            # Recreate and start containers
                            docker-compose down
                            docker-compose up -d
        
                            # Show running containers
                            docker ps
                        EOF
                    """
                }
            }
        }

        stage('DAST - OWASP ZAP') {
            when {
                expression { env.BRANCH_NAME?.startsWith('dev/') }
            }
            steps {
                sh '''
                    chmod 777 $(pwd)
                    docker run -v $(pwd):/zap/wrk/:rw ghcr.io/zaproxy/zaproxy zap-api-scan.py \
                    -t http://<IP>:30000/api-docs/ \
                    -f openapi \
                    -r zap_report.html \
                    -w zap_report.md \
                    -J zap_json_report.json \
                    -x zap_xml_report.xml \
                    -c zap_ignore_rules
                '''
            }
        }

        stage('Approval for Deployment') {
            when {
                branch 'main'
            }
            steps {
                script {
                    def response = input(
                        message: 'Deploy to production?',
                        ok: 'Submit',
                        parameters: [
                            choice(choices: ['Yes', 'No'], name: 'Proceed?', description: 'Choose Yes to deploy')
                        ]
                    )
                    if (response == 'Yes') {
                        echo "✅ Approved for production"
                        deployApproved = true
                    } else {
                        echo "❌ Not approved. Aborting pipeline."
                        currentBuild.result = 'ABORTED'
                        error("Pipeline stopped by user.")
                    }
                }
            }
        }

        stage('Deploy to Production') {
            when {
                expression { return deployApproved }
            }
            steps {
                script {
                    echo "🚀 Deploying to production..."

                    changeImageTag(
                        imageTag: env.IMAGE_TAG,
                        manifestsPath: 'kubernetes',
                        gitCredentials: 'github-credentials',
                        gitUserName: 'Jenkins CI',
                        gitUserEmail: 'jenkins@example.com',
                        repoUrl: 'https://github.com/<your-gitops-repo>/your-app.git'
                    )
                }
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

## 🧭 Installing Argo CD On K8s

```bash
kubectl create namespace argocd
kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml
```
watch every 1 second
```bash
watch -n 1 kubectl get pods -A
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
# Monitoring & Logging
## Prometheus & Grafana
Add the Helm repo
```bash
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo update
```
Install the kube-prometheus-stack
```bash
helm install prometheus-stack prometheus-community/kube-prometheus-stack --namespace monitoring --create-namespace
```
Prometheus UI:
```bash
kubectl port-forward svc/prometheus-stack-kube-prometheus-prometheus -n monitoring 9090 --address=0.0.0.0
```
Visit:
```bash
http://<your-machine-ip>:9090
```
Grafana:
```bash
kubectl port-forward svc/prometheus-stack-grafana -n monitoring 3000 --address=0.0.0.0
```
Visit:
```bash
http://<your-machine-ip>:3000
```
Default login username:
```bash
admin
```
Password: (get from secret) # Default password is prom-operator
```bash
kubectl get secret --namespace monitoring prometheus-stack-grafana -o jsonpath="{.data.admin-password}" | base64 --decode
```
Alertmanager:
```bash
kubectl port-forward svc/kube-prom-stack-alertmanager -n monitoring 9093 --address=0.0.0.0
```
Visit:
```bash
http://<your-machine-ip>:9093
```
---
## ELK Stack
```bash
kubectl create namespace logging
```
Deploy Elasticsearch in logging Namespace
```bash
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: elasticsearch-pvc
  namespace: logging
spec:
  accessModes:
    - ReadWriteOnce
  resources:
    requests:
      storage: 2Gi
  storageClassName: standard
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: elasticsearch
  namespace: logging
spec:
  replicas: 1
  selector:
    matchLabels:
      app: elasticsearch
  template:
    metadata:
      labels:
        app: elasticsearch
    spec:
      containers:
        - name: elasticsearch
          image: docker.elastic.co/elasticsearch/elasticsearch:7.17.0
          env:
            - name: discovery.type
              value: single-node
            - name: ES_JAVA_OPTS
              value: "-Xms512m -Xmx512m"
            - name: xpack.security.enabled
              value: "false"
          ports:
            - containerPort: 9200
          resources:
            limits:
              memory: "2Gi"
              cpu: "1"
            requests:
              memory: "1Gi"
              cpu: "500m"
          volumeMounts:
            - mountPath: /usr/share/elasticsearch/data
              name: elasticsearch-storage
      volumes:
        - name: elasticsearch-storage
          persistentVolumeClaim:
            claimName: elasticsearch-pvc
---
apiVersion: v1
kind: Service
metadata:
  name: elasticsearch
  namespace: logging
spec:
  selector:
    app: elasticsearch
  ports:
    - protocol: TCP
      port: 9200
      targetPort: 9200
```
save as elasticsearch.yaml
```bash
kubectl apply -f elasticsearch.yaml
```
Verify installation
```bash
kubectl get pods -n logging
```
```bash
kubectl get pvc -n logging
```
```bash
kubectl get pv -n logging
```
---
Deploy Kibana in logging Namespace
```bash
apiVersion: apps/v1
kind: Deployment
metadata:
  name: kibana
  namespace: logging
spec:
  replicas: 1
  selector:
    matchLabels:
      app: kibana
  template:
    metadata:
      labels:
        app: kibana
    spec:
      containers:
        - name: kibana
          image: docker.elastic.co/kibana/kibana:7.17.0
          env:
            - name: ELASTICSEARCH_HOSTS
              value: http://elasticsearch:9200
          ports:
            - containerPort: 5601
---
apiVersion: v1
kind: Service
metadata:
  name: kibana
  namespace: logging
spec:
  type: NodePort
  selector:
    app: kibana
  ports:
    - port: 5601
      nodePort: 30601
```
save as kibana.yaml
```bash
kubectl apply -f kibana.yaml
```
```bash
kubectl get pods -n logging
```
---
Access Kibana UI via 
```bash
<your-machine-ip>:30601
```
---
Deploy Logstash
```bash
apiVersion: v1
kind: ConfigMap
metadata:
  name: logstash-config
  namespace: logging
data:
  logstash.conf: |
    input {
      beats {
        port => 5044
      }
    }

    filter {
      # You can add filters like grok, json parsing here if needed later
    }

    output {
      elasticsearch {
        hosts => ["http://elasticsearch.logging.svc.cluster.local:9200"]
        index => "filebeat-%{+YYYY.MM.dd}"
      }
    }
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: logstash
  namespace: logging
spec:
  replicas: 1
  selector:
    matchLabels:
      app: logstash
  template:
    metadata:
      labels:
        app: logstash
    spec:
      containers:
      - name: logstash
        image: docker.elastic.co/logstash/logstash:7.17.0
        ports:
        - containerPort: 5044
        - containerPort: 9600
        volumeMounts:
        - name: config-volume
          mountPath: /usr/share/logstash/pipeline/logstash.conf
          subPath: logstash.conf
      volumes:
      - name: config-volume
        configMap:
          name: logstash-config
          items:
          - key: logstash.conf
            path: logstash.conf
---
apiVersion: v1
kind: Service
metadata:
  name: logstash
  namespace: logging
spec:
  selector:
    app: logstash
  ports:
    - protocol: TCP
      port: 5044
      targetPort: 5044
```
save as logstash.yaml
```bash
kubectl apply -f logstash.yaml
```
To check
```bash
kubectl get all -n logging
```
```bash
watch -n 1 kubectl get po -n logging
```
Deploy Filebeat as Daemonset:
```bash
apiVersion: v1
kind: ConfigMap
metadata:
  name: filebeat-config
  namespace: logging
  labels:
    k8s-app: filebeat
data:
  filebeat.yml: |-
    filebeat.inputs:
    - type: container
      paths:
        - /var/log/containers/*.log
      processors:
        - add_kubernetes_metadata:
            host: ${NODE_NAME}
            matchers:
            - logs_path:
                logs_path: "/var/log/containers/"
    processors:
      - add_cloud_metadata:
      - add_host_metadata:
    output.logstash:
      hosts: ["logstash.logging.svc.cluster.local:5044"]

---
apiVersion: apps/v1
kind: DaemonSet
metadata:
  name: filebeat
  namespace: logging
  labels:
    k8s-app: filebeat
spec:
  selector:
    matchLabels:
      k8s-app: filebeat
  template:
    metadata:
      labels:
        k8s-app: filebeat
    spec:
      serviceAccountName: filebeat
      terminationGracePeriodSeconds: 30
      hostNetwork: true
      dnsPolicy: ClusterFirstWithHostNet
      containers:
      - name: filebeat
        image: docker.elastic.co/beats/filebeat:7.17.28
        args: [
          "-c", "/etc/filebeat.yml",
          "-e",
        ]
        env:
        - name: NODE_NAME
          valueFrom:
            fieldRef:
              fieldPath: spec.nodeName
        securityContext:
          runAsUser: 0
          # If using Red Hat OpenShift uncomment this:
          #privileged: true
        resources:
          limits:
            memory: 200Mi
          requests:
            cpu: 100m
            memory: 100Mi
        volumeMounts:
        - name: config
          mountPath: /etc/filebeat.yml
          readOnly: true
          subPath: filebeat.yml
        - name: data
          mountPath: /usr/share/filebeat/data
        - name: varlibdockercontainers
          mountPath: /var/lib/docker/containers
          readOnly: true
        - name: varlog
          mountPath: /var/log
          readOnly: true
      volumes:
      - name: config
        configMap:
          defaultMode: 0640
          name: filebeat-config
      - name: varlibdockercontainers
        hostPath:
          path: /var/lib/docker/containers
      - name: varlog
        hostPath:
          path: /var/log
      # data folder stores a registry of read status for all files, so we don't send everything again on a Filebeat pod restart
      - name: data
        hostPath:
          # When filebeat runs as non-root user, this directory needs to be writable by group (g+w).
          path: /var/lib/filebeat-data
          type: DirectoryOrCreate
---
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRoleBinding
metadata:
  name: filebeat
subjects:
- kind: ServiceAccount
  name: filebeat
  namespace: logging
roleRef:
  kind: ClusterRole
  name: filebeat
  apiGroup: rbac.authorization.k8s.io
---
apiVersion: rbac.authorization.k8s.io/v1
kind: RoleBinding
metadata:
  name: filebeat
  namespace: logging
subjects:
  - kind: ServiceAccount
    name: filebeat
    namespace: logging
roleRef:
  kind: Role
  name: filebeat
  apiGroup: rbac.authorization.k8s.io
---
apiVersion: rbac.authorization.k8s.io/v1
kind: RoleBinding
metadata:
  name: filebeat-kubeadm-config
  namespace: logging
subjects:
  - kind: ServiceAccount
    name: filebeat
    namespace: logging
roleRef:
  kind: Role
  name: filebeat-kubeadm-config
  apiGroup: rbac.authorization.k8s.io
---
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRole
metadata:
  name: filebeat
  labels:
    k8s-app: filebeat
rules:
- apiGroups: [""] # "" indicates the core API group
  resources:
  - namespaces
  - pods
  - nodes
  verbs:
  - get
  - watch
  - list
- apiGroups: ["apps"]
  resources:
    - replicasets
  verbs: ["get", "list", "watch"]
---
apiVersion: rbac.authorization.k8s.io/v1
kind: Role
metadata:
  name: filebeat
  # should be the namespace where filebeat is running
  namespace: logging
  labels:
    k8s-app: filebeat
rules:
  - apiGroups:
      - coordination.k8s.io
    resources:
      - leases
    verbs: ["get", "create", "update"]
---
apiVersion: rbac.authorization.k8s.io/v1
kind: Role
metadata:
  name: filebeat-kubeadm-config
  namespace: logging
  labels:
    k8s-app: filebeat
rules:
  - apiGroups: [""]
    resources:
      - configmaps
    resourceNames:
      - kubeadm-config
    verbs: ["get"]
---
apiVersion: v1
kind: ServiceAccount
metadata:
  name: filebeat
  namespace: logging
  labels:
    k8s-app: filebeat
---
```
save as filebeat.yaml
```bash
kubectl apply -f filebeat.yaml
```
Verify that Elasticsearch Indices
```bash
kubectl run -i --rm --restart=Never curl --image=curlimages/curl -n logging -- curl http://elasticsearch:9200
kubectl run -i --rm --restart=Never curl --image=curlimages/curl -n logging -- curl http://elasticsearch:9200/_cat/indices?v
```
Verify log collection by Filebeat
```bash
kubectl exec -n logging -it <filebeat-pod> -- ls /var/log/containers/
```
Verify that logstash.conf exists
```bash
kubectl exec -n logging -it <logstash-pod> -- ls /usr/share/logstash/pipeline/
kubectl exec -n logging -it <logstash-pod> -- cat /usr/share/logstash/pipeline/logstash.conf
```
---
### EFK Stack
Create IAM Role for Service Account
```bash
eksctl create iamserviceaccount \
    --name ebs-csi-controller-sa \
    --namespace kube-system \
    --cluster observability \
    --role-name AmazonEKS_EBS_CSI_DriverRole \
    --role-only \
    --attach-policy-arn arn:aws:iam::aws:policy/service-role/AmazonEBSCSIDriverPolicy \
    --approve
```
This command creates an IAM role for the EBS CSI controller.

IAM role allows EBS CSI controller to interact with AWS resources, specifically for managing EBS volumes in the Kubernetes cluster.

We will attach the Role with service account

Retrieve IAM Role ARN
```bash
ARN=$(aws iam get-role --role-name AmazonEKS_EBS_CSI_DriverRole --query 'Role.Arn' --output text)
```
Command retrieves the ARN of the IAM role created for the EBS CSI controller service account.
Deploy EBS CSI Driver
```bash
eksctl create addon --cluster observability --name aws-ebs-csi-driver --version latest \
    --service-account-role-arn $ARN --force
```
Above command deploys the AWS EBS CSI driver as an addon to your Kubernetes cluster.
It uses the previously created IAM service account role to allow the driver to manage EBS volumes securely.
Create Namespace for Logging
```bash
kubectl create namespace logging
```
Install Elasticsearch on K8s
```bash
helm repo add elastic https://helm.elastic.co
helm install elasticsearch \
 --set replicas=1 \
 --set volumeClaimTemplate.storageClassName=gp2 \
 --set persistence.labels.enabled=true elastic/elasticsearch -n logging
```
Installs Elasticsearch in the logging namespace.
It sets the number of replicas, specifies the storage class, and enables persistence labels to ensure data is stored on persistent volumes.
Retrieve Elasticsearch Username & Password
```bash
# for username
kubectl get secrets --namespace=logging elasticsearch-master-credentials -ojsonpath='{.data.username}' | base64 -d
# for password
kubectl get secrets --namespace=logging elasticsearch-master-credentials -ojsonpath='{.data.password}' | base64 -d
```
Retrieves the password for the Elasticsearch cluster's master credentials from the Kubernetes secret.
The password is base64 encoded, so it needs to be decoded before use.
👉 Note: Please write down the password for future reference
Install Kibana
```bash
helm install kibana --set service.type=LoadBalancer elastic/kibana -n logging
```
Kibana provides a user-friendly interface for exploring and visualizing data stored in Elasticsearch.
It is exposed as a LoadBalancer service, making it accessible from outside the cluster.
Install Fluentbit with Custom Values/Configurations
👉 Note: Please update the HTTP_Passwd field in the fluentbit-values.yml file with the password retrieved earlier in step 6: (i.e NJyO47UqeYBsoaEU)"
```bash
helm repo add fluent https://fluent.github.io/helm-charts
helm install fluent-bit fluent/fluent-bit -f fluentbit-values.yaml -n logging
```
---
# Tracing
## Jaeger
### Components of Jaeger
Jaeger consists of several components:

Agent: Collects traces from your application.

Collector: Receives traces from the agent and processes them.

Query: Provides a UI to view traces.

Storage: Stores traces for later retrieval (often a database like Elasticsearch).

Export Elasticsearch CA Certificate
This command retrieves the CA certificate from the Elasticsearch master certificate secret and decodes it, saving it to a ca-cert.pem file.
```bash
kubectl get secret elasticsearch-master-certs -n logging -o jsonpath='{.data.ca\.crt}' | base64 --decode > ca-cert.pem
```
Create Tracing Namespace
```bash
kubectl create ns tracing
```
Create ConfigMap for Jaeger's TLS Certificate
```bash
kubectl create configmap jaeger-tls --from-file=ca-cert.pem -n tracing
```
Create Secret for Elasticsearch TLS
```bash
kubectl create secret generic es-tls-secret --from-file=ca-cert.pem -n tracing
```
Add Jaeger Helm Repository
```bash
helm repo add jaegertracing https://jaegertracing.github.io/helm-charts
helm repo update
```
Install Jaeger with Custom Values
👉 Note: Please update the password field and other related field in the jaeger-values.yaml file with the password retrieved earlier in day-4 at step 6: (i.e NJyO47UqeYBsoaEU)"
Command installs Jaeger into the tracing namespace using a custom jaeger-values.yaml configuration file. Ensure the password is updated in the file before installation.
```bash
helm install jaeger jaegertracing/jaeger -n tracing --values jaeger-values.yaml
```
Port Forward Jaeger Query Service
```bash
kubectl port-forward svc/jaeger-query 8080:80 -n tracing
```
---
# Service Mesh 
## Istio
Install istio using helm
```bash
helm repo add istio https://istio-release.storage.googleapis.com/charts
helm repo update
helm install istio-base istio/base -n istio-system --create-namespace
helm install istiod istio/istiod -n istio-system
```
Enable automatic sidecar injection for the default namespace:
```bash
kubectl label namespace default istio-injection=enabled
```
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
