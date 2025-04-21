package custom.agro.apuracao;

import org.joda.time.LocalDate;
import java.util.List;

import custom.senior.apuracao.Apuracao;
import custom.senior.apuracao.ContextoApuracao;
import com.senior.ContextoGeralRH;
import com.senior.dataset.ICursor;
import com.senior.dataset.IEntity;
import com.senior.dataset.MappedParamProvider;
import com.senior.rh.Colaborador;
import com.senior.rh.ponto.colaborador.Horario;
import com.senior.rh.ponto.apuracao.calculo.TipoHoraFalta;
import com.senior.rh.ponto.apuracao.calculo.TipoIntervalo;
import com.senior.rh.ponto.colaborador.Escala;
import com.senior.rh.entities.readonly.IR066SIT;
import com.senior.rh.ponto.horas.ISeparacaoHoras;
import com.senior.rh.ponto.marcacoes.Marcacao;
import com.senior.rh.ponto.marcacoes.MarcacaoRegra;
import com.senior.rule.Rule;
import com.senior.rh.ponto.apuracao.calculo.IntervaloCalculo;
import com.senior.rh.ponto.apuracao.calculo.TipoHoraExtra;


@Rule(description = "Regra de Apuração.")
public class RegraApuracao extends Apuracao {

	public void execute() {

		// Obtem contextos
		ContextoGeralRH geral = getContainer().getContextoGeral();
		ContextoApuracao apuracao = getContainer().getContextoApuracao();
		Colaborador colaborador = apuracao.getColaborador();

		// Obtem variáveis do colaborador/dia
		int numEmp = colaborador.getNumeroEmpresa();
		int tipCol = colaborador.getTipoColaborador();
		int numCad = colaborador.getNumCad();
		int tipCon = colaborador.getTipoContrato();
		Horario horario = apuracao.getHorario();
		Escala escala = apuracao.getEscala();
		LocalDate datPro = apuracao.getData();
		int codHor = horario.getCodigo();
		int codEsc = escala.getCodigo();

		// Inicialização variáveis
		MappedParamProvider paramProvider = new MappedParamProvider();

		// Trata a apuração para que somente dê falta quando for a carga horária cheia do dia
		ISeparacaoHoras prvTra = apuracao.getTotalMinutosPrevisto(codHor);
		if (apuracao.getHorSit(100) == prvTra.getTotalHoras()) {
			apuracao.setHorSit(15, apuracao.getHorSit(100));
			apuracao.zeraHorasSituacao(100);
		}

		// Trata a apuração quando houver atestado e horas trabalhadas no mesmo dia
		int codSit = 0;
		int qtdRes = 0;
		if (apuracao.getHorSit(14) > 0 && apuracao.getHorSit(1) > 0) {
			// Busca as demais situações apuradas
			paramProvider.setParam("numEmp", numEmp);
			paramProvider.setParam("tipCol", tipCol);
			paramProvider.setParam("numCad", numCad);
			paramProvider.setParam("datApu", datPro);
			IR066SIT c066Sit = consulta(IR066SIT.class, "NumEmp=:numEmp AND " + "TipCol=:tipCol AND "
					+ "NumCad=:numCad AND " + "DatApu=:datApu AND " + "CodSit NOT IN (1, 14)", paramProvider);
			if (c066Sit != null)
				codSit = c066Sit.getCodSit();

			ISeparacaoHoras totTra = apuracao.getHorasTrabalhadas();
			apuracao.setHorSit(1, totTra.getTotalHoras());
			apuracao.setHorSit(14, prvTra.getTotalHoras() - apuracao.getHorSit(1));

			qtdRes = totTra.getTotalHoras() - (apuracao.getHorSit(1) + apuracao.getHorSit(14));

			// CH - 223165 -- CodSit = 0
			if (qtdRes >= 0) {
		        apuracao.setHorSit(codSit, qtdRes);
		    } else {
		        // Verifica se codSit é diferente de 0 antes de chamar zeraHorasSituacao
		        if (codSit != 0) {
		            apuracao.zeraHorasSituacao(codSit);
		        }
		    }

		}

		// Trata a apuração dos aprendizes
		if (tipCon == 6) {
			int diaCur1 = 0;
			int diaCur2 = 0;
			paramProvider.setParam("numEmp", numEmp);
			paramProvider.setParam("tipCol", tipCol);
			paramProvider.setParam("numCad", numCad);
			USU_ICurApr cCurApr = consulta(USU_ICurApr.class, "USU_NumEmp=:numEmp AND " + "USU_TipCol=:tipCol AND "
					+ "USU_NumCad=:numCad", paramProvider);
			if (cCurApr != null) {
				diaCur1 = cCurApr.getUSU_DiSem1();
				diaCur2 = cCurApr.getUSU_DiSem2();
			}

			int diaSem = geral.getDiaSem(datPro);
			//Domingo
		    if (diaSem == 0 && (diaCur1 == 1 || diaCur2 == 1)) {
		    	apuracao.setHorSit(78, apuracao.getHorSit(15));
		        apuracao.zeraHorasSituacao(15);
		    }
		    // Segunda
		    if (diaSem == 1 && (diaCur1 == 2 || diaCur2 == 2)) {
		    	apuracao.setHorSit(78, apuracao.getHorSit(15));
		        apuracao.zeraHorasSituacao(15);
		    }
		    // Terça
		    if (diaSem == 2 && (diaCur1 == 3 || diaCur2 == 3)) {
		    	apuracao.setHorSit(78, apuracao.getHorSit(15));
		        apuracao.zeraHorasSituacao(15);
		    }
		    // Quarta
		    if (diaSem == 3 && (diaCur1 == 4 || diaCur2 == 4)) {
		    	apuracao.setHorSit(78, apuracao.getHorSit(15));
		        apuracao.zeraHorasSituacao(15);
		    }
		    // Quinta
		    if (diaSem == 4 && (diaCur1 == 5 || diaCur2 == 5)) {
		    	apuracao.setHorSit(78, apuracao.getHorSit(15));
		        apuracao.zeraHorasSituacao(15);
		    }
 		    // Sexta
		    if (diaSem == 5 && (diaCur1 == 6 || diaCur2 == 6)) {
		    	apuracao.setHorSit(78, apuracao.getHorSit(15));
		        apuracao.zeraHorasSituacao(15);
		    }
		    // Sábado
		    if (diaSem == 6 && (diaCur1 == 7 || diaCur2 == 7)) {
		    	apuracao.setHorSit(78, apuracao.getHorSit(15));
		        apuracao.zeraHorasSituacao(15);
		    }
		}

		//Trata as marcações ímpares, inválidas
		int qtdMar = apuracao.getQtdMarcacoesRealizadas(true);
		if(qtdMar % 2 != 0){
			int iHorEnt = 0;
			int iHorTot = 0;
			int iHorSai = 0;
			int iFluxo = 0;
			List<MarcacaoRegra> ListaMarcacoes = apuracao.getMarcacoesRealizadas(true);
			for (int i = 0; i < ListaMarcacoes.size(); i++) {
				if(i % 2 == 0 && iFluxo == 0){//marcações 0,2,4
					iHorEnt = ListaMarcacoes.get(i).getHora();
				} else {
					if(ListaMarcacoes.get(i).getHora() < iHorEnt){
						iHorSai = ListaMarcacoes.get(i).getHora()  + 1440;
					} else {
						iHorSai = ListaMarcacoes.get(i).getHora();
					}
					if (iHorSai - iHorEnt < 120){
						iHorEnt = iHorSai;
						iFluxo = 1;
					} else {
						iHorTot = iHorTot + iHorSai - iHorEnt;
					}
				}

			}
			apuracao.zeraHorasSituacao(999);
			apuracao.setHorSit(1,iHorTot);
			int iHorPre = prvTra.getTotalHoras();
			if(iHorTot >= iHorPre){
				apuracao.zeraHorasSituacao(1);
				apuracao.setHorSit(999,iHorTot);
			}
			if(iHorPre > iHorTot){
				apuracao.setHorSit(1,iHorTot);
				apuracao.setHorSit(100,iHorPre - iHorTot);
			}
			if(iHorTot == 0){
				apuracao.setHorSit(100,iHorPre);
			}
		}
	}

	// Método genérico para fazer um cursor
	private <C extends IEntity> C consulta(Class<C> entidade, String filtro, MappedParamProvider paramProvider) {
		ICursor<C> cursor = getContainer().getEntitySession().newCursor(entidade);
		if (filtro != "")
			cursor.addFilter(filtro, paramProvider);
		cursor.open();
		try {
			if (cursor.first()) {
				C ler = cursor.read();
				return ler;
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			cursor.close();
		}
		return null;
	}

}